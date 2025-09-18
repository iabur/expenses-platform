package com.expenses.svcexpense.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "expense_line_items")
public class ExpenseLineItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "expense_id", nullable = false)
  private Expense expense;

  @Column(nullable = false)
  @NotBlank(message = "Description is required")
  private String description;

  @Column(precision = 10, scale = 3, nullable = false)
  @Positive(message = "Quantity must be positive")
  private BigDecimal quantity = BigDecimal.ONE;

  @Column(name = "unit_price_cents", nullable = false)
  @Positive(message = "Unit price must be positive")
  private Long unitPriceCents;

  @Column(name = "total_price_cents", nullable = false)
  @Positive(message = "Total price must be positive")
  private Long totalPriceCents;

  @Column(length = 50)
  private String category;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  // Constructors
  public ExpenseLineItem() {
  }

  public ExpenseLineItem(Expense expense, String description, BigDecimal quantity, Long unitPriceCents) {
    this.expense = expense;
    this.description = description;
    this.quantity = quantity;
    this.unitPriceCents = unitPriceCents;
    this.totalPriceCents = quantity.multiply(BigDecimal.valueOf(unitPriceCents)).longValue();
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
    updateTotalPrice();
  }

  public Long getUnitPriceCents() {
    return unitPriceCents;
  }

  public void setUnitPriceCents(Long unitPriceCents) {
    this.unitPriceCents = unitPriceCents;
    updateTotalPrice();
  }

  public Long getTotalPriceCents() {
    return totalPriceCents;
  }

  public void setTotalPriceCents(Long totalPriceCents) {
    this.totalPriceCents = totalPriceCents;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  // Helper methods
  public BigDecimal getUnitPriceDecimal() {
    return BigDecimal.valueOf(unitPriceCents, 2);
  }

  public void setUnitPriceDecimal(BigDecimal unitPrice) {
    this.unitPriceCents = unitPrice.movePointRight(2).longValue();
    updateTotalPrice();
  }

  public BigDecimal getTotalPriceDecimal() {
    return BigDecimal.valueOf(totalPriceCents, 2);
  }

  public void setTotalPriceDecimal(BigDecimal totalPrice) {
    this.totalPriceCents = totalPrice.movePointRight(2).longValue();
  }

  private void updateTotalPrice() {
    if (quantity != null && unitPriceCents != null) {
      this.totalPriceCents = quantity.multiply(BigDecimal.valueOf(unitPriceCents)).longValue();
    }
  }

  @PrePersist
  @PreUpdate
  private void calculateTotalPrice() {
    updateTotalPrice();
  }
}
