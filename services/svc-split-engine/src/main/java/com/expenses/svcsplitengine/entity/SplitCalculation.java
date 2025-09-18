package com.expenses.svcsplitengine.entity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "split_calculations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitCalculation {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @Column(name = "expense_id", nullable = false)
  private UUID expenseId;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "total_amount_cents", nullable = false)
  private Long totalAmountCents;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "split_method", nullable = false, length = 20)
  private SplitMethod splitMethod;

  @Enumerated(EnumType.STRING)
  @Column(name = "calculation_status", nullable = false, length = 20)
  @Builder.Default
  private CalculationStatus calculationStatus = CalculationStatus.PENDING;

  @Column(name = "created_at", nullable = false)
  private ZonedDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "calculated_at")
  private ZonedDateTime calculatedAt;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @OneToMany(mappedBy = "splitCalculation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<ParticipantSplit> participantSplits = new ArrayList<>();

  public enum SplitMethod {
    EQUAL,
    PERCENTAGE,
    EXACT_AMOUNTS,
    SHARES
  }

  public enum CalculationStatus {
    PENDING,
    CALCULATED,
    FAILED
  }

  @PrePersist
  protected void onCreate() {
    ZonedDateTime now = ZonedDateTime.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = ZonedDateTime.now();
  }

  // Helper methods
  public void addParticipantSplit(ParticipantSplit participantSplit) {
    participantSplits.add(participantSplit);
    participantSplit.setSplitCalculation(this);
  }

  public void markAsCalculated() {
    this.calculationStatus = CalculationStatus.CALCULATED;
    this.calculatedAt = ZonedDateTime.now();
    this.errorMessage = null;
  }

  public void markAsFailed(String errorMessage) {
    this.calculationStatus = CalculationStatus.FAILED;
    this.errorMessage = errorMessage;
    this.calculatedAt = null;
  }

  public boolean isCalculated() {
    return calculationStatus == CalculationStatus.CALCULATED;
  }

  public boolean isPending() {
    return calculationStatus == CalculationStatus.PENDING;
  }

  public boolean hasFailed() {
    return calculationStatus == CalculationStatus.FAILED;
  }
}
