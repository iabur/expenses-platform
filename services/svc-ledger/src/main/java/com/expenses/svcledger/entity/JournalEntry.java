package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
public class JournalEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "reference_type", length = 20, nullable = false)
  private ReferenceType referenceType;

  @Column(name = "reference_id", nullable = false)
  @NotNull(message = "Reference ID is required")
  private UUID referenceId;

  @Column(name = "group_id", nullable = false)
  @NotNull(message = "Group ID is required")
  private UUID groupId;

  @Column(columnDefinition = "TEXT", nullable = false)
  @NotBlank(message = "Description is required")
  private String description;

  @Column(name = "value_date", nullable = false)
  @NotNull(message = "Value date is required")
  private LocalDate valueDate;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @Column(name = "created_by", nullable = false)
  @NotNull(message = "Created by user ID is required")
  private UUID createdBy;

  @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Posting> postings = new ArrayList<>();

  // Constructors
  public JournalEntry() {
  }

  public JournalEntry(ReferenceType referenceType, UUID referenceId, UUID groupId,
      String description, LocalDate valueDate, UUID createdBy) {
    this.referenceType = referenceType;
    this.referenceId = referenceId;
    this.groupId = groupId;
    this.description = description;
    this.valueDate = valueDate;
    this.createdBy = createdBy;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ReferenceType getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(ReferenceType referenceType) {
    this.referenceType = referenceType;
  }

  public UUID getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(UUID referenceId) {
    this.referenceId = referenceId;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public void setGroupId(UUID groupId) {
    this.groupId = groupId;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getValueDate() {
    return valueDate;
  }

  public void setValueDate(LocalDate valueDate) {
    this.valueDate = valueDate;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public List<Posting> getPostings() {
    return postings;
  }

  public void setPostings(List<Posting> postings) {
    this.postings = postings;
  }

  // Helper methods
  public void addPosting(Posting posting) {
    postings.add(posting);
    posting.setJournalEntry(this);
  }

  public void removePosting(Posting posting) {
    postings.remove(posting);
    posting.setJournalEntry(null);
  }

  public long getTotalDebits() {
    return postings.stream()
        .filter(p -> p.getDebitAccount() != null)
        .mapToLong(Posting::getAmountCents)
        .sum();
  }

  public long getTotalCredits() {
    return postings.stream()
        .filter(p -> p.getCreditAccount() != null)
        .mapToLong(Posting::getAmountCents)
        .sum();
  }

  public boolean isBalanced() {
    return getTotalDebits() == getTotalCredits();
  }

  // Reference type enum
  public enum ReferenceType {
    EXPENSE, PAYMENT, SETTLEMENT
  }
}
