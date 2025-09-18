package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "postings")
public class Posting {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "journal_entry_id", nullable = false)
  private JournalEntry journalEntry;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "debit_account_id")
  private Account debitAccount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "credit_account_id")
  private Account creditAccount;

  @Column(name = "amount_cents", nullable = false)
  @Positive(message = "Amount must be positive")
  private Long amountCents;

  @Column(length = 3, nullable = false)
  @NotNull(message = "Currency is required")
  private String currency;

  @Column(name = "fx_rate", precision = 18, scale = 8)
  private BigDecimal fxRate;

  @Column(columnDefinition = "TEXT")
  private String narrative;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  // Constructors
  public Posting() {
  }

  public Posting(JournalEntry journalEntry, Account debitAccount, Account creditAccount,
      Long amountCents, String currency) {
    this.journalEntry = journalEntry;
    this.debitAccount = debitAccount;
    this.creditAccount = creditAccount;
    this.amountCents = amountCents;
    this.currency = currency;
  }

  public Posting(JournalEntry journalEntry, Account debitAccount, Account creditAccount,
      Long amountCents, String currency, String narrative) {
    this.journalEntry = journalEntry;
    this.debitAccount = debitAccount;
    this.creditAccount = creditAccount;
    this.amountCents = amountCents;
    this.currency = currency;
    this.narrative = narrative;
  }

  // Static factory methods for creating debit/credit postings
  public static Posting debit(JournalEntry journalEntry, Account account, Long amountCents, String currency,
      String narrative) {
    return new Posting(journalEntry, account, null, amountCents, currency, narrative);
  }

  public static Posting credit(JournalEntry journalEntry, Account account, Long amountCents, String currency,
      String narrative) {
    return new Posting(journalEntry, null, account, amountCents, currency, narrative);
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public JournalEntry getJournalEntry() {
    return journalEntry;
  }

  public void setJournalEntry(JournalEntry journalEntry) {
    this.journalEntry = journalEntry;
  }

  public Account getDebitAccount() {
    return debitAccount;
  }

  public void setDebitAccount(Account debitAccount) {
    this.debitAccount = debitAccount;
  }

  public Account getCreditAccount() {
    return creditAccount;
  }

  public void setCreditAccount(Account creditAccount) {
    this.creditAccount = creditAccount;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(Long amountCents) {
    this.amountCents = amountCents;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public BigDecimal getFxRate() {
    return fxRate;
  }

  public void setFxRate(BigDecimal fxRate) {
    this.fxRate = fxRate;
  }

  public String getNarrative() {
    return narrative;
  }

  public void setNarrative(String narrative) {
    this.narrative = narrative;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // Helper methods
  public BigDecimal getAmountDecimal() {
    return BigDecimal.valueOf(amountCents, 2);
  }

  public void setAmountDecimal(BigDecimal amount) {
    this.amountCents = amount.movePointRight(2).longValue();
  }

  public boolean isDebit() {
    return debitAccount != null;
  }

  public boolean isCredit() {
    return creditAccount != null;
  }

  public Account getAccount() {
    return isDebit() ? debitAccount : creditAccount;
  }

  public PostingType getType() {
    return isDebit() ? PostingType.DEBIT : PostingType.CREDIT;
  }

  /**
   * Get the effect on account balance (positive for debit, negative for credit)
   */
  public long getBalanceEffect() {
    return isDebit() ? amountCents : -amountCents;
  }

  // Validation method
  @PrePersist
  @PreUpdate
  private void validate() {
    if ((debitAccount == null) == (creditAccount == null)) {
      throw new IllegalStateException("Posting must have exactly one of debit or credit account");
    }
  }

  // Posting type enum
  public enum PostingType {
    DEBIT, CREDIT
  }
}
