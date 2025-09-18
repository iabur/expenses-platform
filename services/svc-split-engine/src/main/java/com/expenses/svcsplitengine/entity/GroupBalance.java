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
@Table(name = "group_balances", uniqueConstraints = @UniqueConstraint(columnNames = { "group_id", "user_id",
    "currency" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupBalance {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "balance_cents", nullable = false)
  @Builder.Default
  private Long balanceCents = 0L;

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
  public BigDecimal getBalanceDecimal() {
    return BigDecimal.valueOf(balanceCents).divide(BigDecimal.valueOf(100));
  }

  public void setBalanceDecimal(BigDecimal balance) {
    this.balanceCents = balance.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public void addToBalance(BigDecimal amount) {
    this.balanceCents += amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public void addToBalance(Long amountCents) {
    this.balanceCents += amountCents;
  }

  public void subtractFromBalance(BigDecimal amount) {
    this.balanceCents -= amount.multiply(BigDecimal.valueOf(100)).longValue();
  }

  public void subtractFromBalance(Long amountCents) {
    this.balanceCents -= amountCents;
  }

  // Balance interpretation
  public boolean owesMoneyToGroup() {
    return balanceCents < 0;
  }

  public boolean isOwedMoneyByGroup() {
    return balanceCents > 0;
  }

  public boolean isBalanced() {
    return balanceCents == 0;
  }

  public BigDecimal getAbsoluteBalance() {
    return getBalanceDecimal().abs();
  }

  public Long getAbsoluteBalanceCents() {
    return Math.abs(balanceCents);
  }
}
