package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "account_balances")
public class AccountBalance {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "account_id", nullable = false, unique = true)
  private Account account;

  @Column(name = "balance_cents", nullable = false)
  private Long balanceCents = 0L;

  @Column(length = 3, nullable = false)
  @NotNull(message = "Currency is required")
  private String currency;

  @UpdateTimestamp
  @Column(name = "last_updated", nullable = false)
  private ZonedDateTime lastUpdated;

  // Constructors
  public AccountBalance() {
  }

  public AccountBalance(Account account, String currency) {
    this.account = account;
    this.currency = currency;
    this.balanceCents = 0L;
  }

  public AccountBalance(Account account, Long balanceCents, String currency) {
    this.account = account;
    this.balanceCents = balanceCents;
    this.currency = currency;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public Long getBalanceCents() {
    return balanceCents;
  }

  public void setBalanceCents(Long balanceCents) {
    this.balanceCents = balanceCents;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public ZonedDateTime getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(ZonedDateTime lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  // Helper methods
  public BigDecimal getBalanceDecimal() {
    return BigDecimal.valueOf(balanceCents, 2);
  }

  public void setBalanceDecimal(BigDecimal balance) {
    this.balanceCents = balance.movePointRight(2).longValue();
  }

  public void addToBalance(Long amountCents) {
    this.balanceCents += amountCents;
  }

  public void subtractFromBalance(Long amountCents) {
    this.balanceCents -= amountCents;
  }

  public void addToBalance(BigDecimal amount) {
    addToBalance(amount.movePointRight(2).longValue());
  }

  public void subtractFromBalance(BigDecimal amount) {
    subtractFromBalance(amount.movePointRight(2).longValue());
  }

  public boolean isPositive() {
    return balanceCents > 0;
  }

  public boolean isNegative() {
    return balanceCents < 0;
  }

  public boolean isZero() {
    return balanceCents == 0;
  }

  public BigDecimal getAbsoluteBalance() {
    return BigDecimal.valueOf(Math.abs(balanceCents), 2);
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
      return "owed " + getBalanceDecimal().abs() + " " + currency;
    } else {
      return "owes " + getBalanceDecimal().abs() + " " + currency;
    }
  }

  /**
   * Reset balance to zero
   */
  public void reset() {
    this.balanceCents = 0L;
  }

  /**
   * Update balance with a posting effect
   */
  public void applyPosting(Posting posting) {
    if (posting.isDebit() && posting.getDebitAccount().equals(account)) {
      addToBalance(posting.getAmountCents());
    } else if (posting.isCredit() && posting.getCreditAccount().equals(account)) {
      subtractFromBalance(posting.getAmountCents());
    }
  }
}
