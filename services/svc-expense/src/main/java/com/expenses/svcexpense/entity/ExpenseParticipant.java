package com.expenses.svcexpense.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "expense_participants", uniqueConstraints = @UniqueConstraint(columnNames = { "expense_id", "user_id" }))
public class ExpenseParticipant {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id", nullable = false)
  private Expense expense;

  @Column(name = "user_id", nullable = false)
  @NotNull(message = "User ID is required")
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "rule_type", length = 20, nullable = false)
  private SplitRuleType ruleType;

  @Column(name = "rule_value", precision = 18, scale = 8)
  private BigDecimal ruleValue;

  @Column(name = "calculated_amount_cents", nullable = false)
  private Long calculatedAmountCents = 0L;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Constructors
  public ExpenseParticipant() {
  }

  public ExpenseParticipant(Expense expense, UUID userId, SplitRuleType ruleType) {
    this.expense = expense;
    this.userId = userId;
    this.ruleType = ruleType;
  }

  public ExpenseParticipant(Expense expense, UUID userId, SplitRuleType ruleType, BigDecimal ruleValue) {
    this.expense = expense;
    this.userId = userId;
    this.ruleType = ruleType;
    this.ruleValue = ruleValue;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Expense getExpense() {
    return expense;
  }

  public void setExpense(Expense expense) {
    this.expense = expense;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public SplitRuleType getRuleType() {
    return ruleType;
  }

  public void setRuleType(SplitRuleType ruleType) {
    this.ruleType = ruleType;
  }

  public BigDecimal getRuleValue() {
    return ruleValue;
  }

  public void setRuleValue(BigDecimal ruleValue) {
    this.ruleValue = ruleValue;
  }

  public Long getCalculatedAmountCents() {
    return calculatedAmountCents;
  }

  public void setCalculatedAmountCents(Long calculatedAmountCents) {
    this.calculatedAmountCents = calculatedAmountCents;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(ZonedDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  // Helper methods
  public BigDecimal getCalculatedAmountDecimal() {
    return BigDecimal.valueOf(calculatedAmountCents, 2);
  }

  public void setCalculatedAmountDecimal(BigDecimal amount) {
    this.calculatedAmountCents = amount.movePointRight(2).longValue();
  }

  // Split rule type enum
  public enum SplitRuleType {
    EQUAL, // Split equally among participants
    PERCENT, // Split by percentage (ruleValue = percentage 0-100)
    SHARES, // Split by shares (ruleValue = number of shares)
    FIXED // Fixed amount (ruleValue = amount in expense currency)
  }

  /**
   * JPA lifecycle callback - set timestamps before persisting
   */
  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = ZonedDateTime.now();
    }
    if (updatedAt == null) {
      updatedAt = ZonedDateTime.now();
    }
  }

  /**
   * JPA lifecycle callback - update timestamp before updating
   */
  @PreUpdate
  protected void onUpdate() {
    updatedAt = ZonedDateTime.now();
  }
}
