package com.expenses.svcsplitengine.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_debts", uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "debtor_id",
    "creditor_id", "currency" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDebt {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "debtor_id", nullable = false)
  private UUID debtorId; // who owes money

  @Column(name = "creditor_id", nullable = false)
  private UUID creditorId; // who is owed money

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "amount_cents", nullable = false)
  private Long amountCents;

  @Column(name = "last_updated", nullable = false)
  private ZonedDateTime lastUpdated;

  @Column(name = "last_expense_id")
  private UUID lastExpenseId;

  @PrePersist
  @PreUpdate
  protected void onUpdate() {
    lastUpdated = ZonedDateTime.now();
  }

  // Helper methods for decimal operations
  public BigDecimal getAmountDecimal() {
    return BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100));
  }

  public void setAmountDecimal(BigDecimal amount) {
    this.amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public void addToAmount(BigDecimal amount) {
    this.amountCents += amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public void addToAmount(Long amountCents) {
    this.amountCents += amountCents;
  }

  public void subtractFromAmount(BigDecimal amount) {
    Long subtractCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
    this.amountCents = Math.max(0, this.amountCents - subtractCents);
  }

  public void subtractFromAmount(Long amountCents) {
    this.amountCents = Math.max(0, this.amountCents - amountCents);
  }

  // Business logic
  public boolean hasDebt() {
    return amountCents > 0;
  }

  public boolean isSettled() {
    return amountCents == 0;
  }

  public boolean canSettle(BigDecimal paymentAmount) {
    return paymentAmount.compareTo(getAmountDecimal()) >= 0;
  }

  public boolean canPartiallySettle(BigDecimal paymentAmount) {
    return paymentAmount.compareTo(BigDecimal.ZERO) > 0 &&
        paymentAmount.compareTo(getAmountDecimal()) < 0;
  }

  /**
   * Creates reverse debt relationship for bidirectional debt tracking
   */
  public UserDebt createReverse() {
    return UserDebt.builder()
        .groupId(this.groupId)
        .debtorId(this.creditorId)
        .creditorId(this.debtorId)
        .currency(this.currency)
        .amountCents(this.amountCents)
        .lastExpenseId(this.lastExpenseId)
        .build();
  }
}
