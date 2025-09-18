package com.expenses.svcexpense.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "expenses")
public class Expense {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false)
  @NotNull(message = "Group ID is required")
  private UUID groupId;

  @Column(name = "creator_id", nullable = false)
  @NotNull(message = "Creator ID is required")
  private UUID creatorId;

  @Column(length = 3, nullable = false)
  @NotBlank(message = "Currency is required")
  private String currency;

  @Column(name = "amount_cents", nullable = false)
  @Positive(message = "Amount must be positive")
  private Long amountCents;

  @Column(name = "occurred_at", nullable = false)
  @NotNull(message = "Occurred date is required")
  private LocalDate occurredAt;

  @Column(columnDefinition = "TEXT")
  private String note;

  @Column(length = 50)
  private String category;

  @Column(name = "fx_rate", precision = 18, scale = 8)
  private BigDecimal fxRate;

  @Column(name = "fx_base_currency", length = 3)
  private String fxBaseCurrency;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "is_deleted", nullable = false)
  private Boolean isDeleted = false;

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ExpenseParticipant> participants = new HashSet<>();

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ExpenseLineItem> lineItems = new HashSet<>();

  @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<ExpenseAttachment> attachments = new HashSet<>();

  // Constructors
  public Expense() {
  }

  public Expense(UUID groupId, UUID creatorId, String currency, Long amountCents, LocalDate occurredAt) {
    this.groupId = groupId;
    this.creatorId = creatorId;
    this.currency = currency;
    this.amountCents = amountCents;
    this.occurredAt = occurredAt;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public UUID getCreatorId() {
    return creatorId;
  }

  public void setCreatorId(UUID creatorId) {
    this.creatorId = creatorId;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Long getAmountCents() {
    return amountCents;
  }

  public void setAmountCents(Long amountCents) {
    this.amountCents = amountCents;
  }

  public LocalDate getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDate occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public BigDecimal getFxRate() {
    return fxRate;
  }

  public void setFxRate(BigDecimal fxRate) {
    this.fxRate = fxRate;
  }

  public String getFxBaseCurrency() {
    return fxBaseCurrency;
  }

  public void setFxBaseCurrency(String fxBaseCurrency) {
    this.fxBaseCurrency = fxBaseCurrency;
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

  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(Boolean isDeleted) {
    this.isDeleted = isDeleted;
  }

  public Set<ExpenseParticipant> getParticipants() {
    return participants;
  }

  public void setParticipants(Set<ExpenseParticipant> participants) {
    this.participants = participants;
  }

  public Set<ExpenseLineItem> getLineItems() {
    return lineItems;
  }

  public void setLineItems(Set<ExpenseLineItem> lineItems) {
    this.lineItems = lineItems;
  }

  public Set<ExpenseAttachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(Set<ExpenseAttachment> attachments) {
    this.attachments = attachments;
  }

  // Helper methods
  public BigDecimal getAmountDecimal() {
    return BigDecimal.valueOf(amountCents, 2);
  }

  public void setAmountDecimal(BigDecimal amount) {
    this.amountCents = amount.movePointRight(2).longValue();
  }

  public void addParticipant(ExpenseParticipant participant) {
    participants.add(participant);
    participant.setExpense(this);
  }

  public void removeParticipant(ExpenseParticipant participant) {
    participants.remove(participant);
    participant.setExpense(null);
  }

  public void addLineItem(ExpenseLineItem lineItem) {
    lineItems.add(lineItem);
    lineItem.setExpense(this);
  }

  public void removeLineItem(ExpenseLineItem lineItem) {
    lineItems.remove(lineItem);
    lineItem.setExpense(null);
  }

  public void addAttachment(ExpenseAttachment attachment) {
    attachments.add(attachment);
    attachment.setExpense(this);
  }

  public void removeAttachment(ExpenseAttachment attachment) {
    attachments.remove(attachment);
    attachment.setExpense(null);
  }

  public void softDelete() {
    this.isDeleted = true;
  }

  public boolean isActive() {
    return !isDeleted;
  }
}
