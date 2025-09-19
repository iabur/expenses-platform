package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "account" })
@ToString(exclude = { "account" })
public class AccountBalance {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @OneToOne
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  @NotNull(message = "Account is required")
  private Account account;

  @Builder.Default
  @Column(name = "balance_cents", nullable = false)
  @NotNull(message = "Balance is required")
  private Long balanceCents = 0L;

  @UpdateTimestamp
  @Column(name = "last_updated", nullable = false)
  private ZonedDateTime lastUpdated;

  @Column(name = "last_transaction_id")
  private UUID lastTransactionId;

  @Column(name = "version", nullable = false)
  @Version
  private Long version;

  // Helper methods
  public BigDecimal getBalanceDecimal() {
    return balanceCents != null ? BigDecimal.valueOf(balanceCents, 2) : BigDecimal.ZERO;
  }

  public void setBalanceDecimal(BigDecimal balance) {
    this.balanceCents = balance != null ? balance.movePointRight(2).longValue() : 0L;
  }

  public void addToBalance(Long amountCents) {
    this.balanceCents = (this.balanceCents != null ? this.balanceCents : 0L) + amountCents;
  }

  public void subtractFromBalance(Long amountCents) {
    this.balanceCents = (this.balanceCents != null ? this.balanceCents : 0L) - amountCents;
  }

  public void addToBalance(BigDecimal amount) {
    if (amount != null) {
      addToBalance(amount.movePointRight(2).longValue());
    }
  }

  public void subtractFromBalance(BigDecimal amount) {
    if (amount != null) {
      subtractFromBalance(amount.movePointRight(2).longValue());
    }
  }

  public boolean isPositive() {
    return balanceCents != null && balanceCents > 0;
  }

  public boolean isNegative() {
    return balanceCents != null && balanceCents < 0;
  }

  public boolean isZero() {
    return balanceCents != null && balanceCents == 0;
  }

  public String getFormattedBalance() {
    if (balanceCents == null)
      return "0.00";
    return String.format("%.2f", balanceCents / 100.0);
  }

  public String getBalanceWithCurrency() {
    String currency = account != null ? account.getCurrency() : "USD";
    return String.format("%s %s", getFormattedBalance(), currency);
  }

  public BigDecimal getAbsoluteBalance() {
    return balanceCents != null ? BigDecimal.valueOf(Math.abs(balanceCents), 2) : BigDecimal.ZERO;
  }

  /**
   * For user accounts: positive balance means they are owed money (others owe
   * them)
   * For user accounts: negative balance means they owe money to others
   * For group accounts: should typically be zero after all expenses are split
   */
  public String getBalanceDescription() {
    if (isZero()) {
      return "settled";
    } else if (isPositive()) {
      return "owed " + getAbsoluteBalance() + " " + (account != null ? account.getCurrency() : "");
    } else {
      return "owes " + getAbsoluteBalance() + " " + (account != null ? account.getCurrency() : "");
    }
  }

  // Business methods for double-entry bookkeeping
  public void debit(Long amountCents, UUID transactionId) {
    addToBalance(amountCents);
    this.lastTransactionId = transactionId;
  }

  public void credit(Long amountCents, UUID transactionId) {
    subtractFromBalance(amountCents);
    this.lastTransactionId = transactionId;
  }

  public void resetBalance() {
    this.balanceCents = 0L;
    this.lastTransactionId = null;
  }

  /**
   * Update balance with a posting effect
   */
  public void applyPosting(Posting posting) {
    if (posting == null)
      return;

    if (posting.isDebit() && posting.getDebitAccount().equals(account)) {
      debit(posting.getAmountCents(), posting.getJournalEntry().getId());
    } else if (posting.isCredit() && posting.getCreditAccount().equals(account)) {
      credit(posting.getAmountCents(), posting.getJournalEntry().getId());
    }
  }
}