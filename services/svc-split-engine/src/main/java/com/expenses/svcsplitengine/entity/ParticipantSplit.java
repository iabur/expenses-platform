package com.expenses.svcsplitengine.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "participant_splits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantSplit {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "split_calculation_id", nullable = false)
  private SplitCalculation splitCalculation;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "split_rule_type", nullable = false, length = 20)
  private SplitRuleType splitRuleType;

  @Column(name = "split_rule_value", precision = 19, scale = 4)
  private BigDecimal splitRuleValue;

  @Column(name = "calculated_amount_cents")
  private Long calculatedAmountCents;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "created_at", nullable = false)
  private ZonedDateTime createdAt;

  public enum SplitRuleType {
    EQUAL,
    PERCENTAGE,
    EXACT_AMOUNT,
    SHARES
  }

  @PrePersist
  protected void onCreate() {
    createdAt = ZonedDateTime.now();
  }

  // Helper methods
  public BigDecimal getCalculatedAmountDecimal() {
    return calculatedAmountCents != null ? BigDecimal.valueOf(calculatedAmountCents).divide(BigDecimal.valueOf(100))
        : BigDecimal.ZERO;
  }

  public void setCalculatedAmountDecimal(BigDecimal amount) {
    this.calculatedAmountCents = amount != null ? amount.multiply(BigDecimal.valueOf(100)).longValue() : null;
  }

  public BigDecimal getSplitRuleValueOrDefault() {
    return splitRuleValue != null ? splitRuleValue : BigDecimal.ZERO;
  }

  public boolean isEqualSplit() {
    return splitRuleType == SplitRuleType.EQUAL;
  }

  public boolean isPercentageSplit() {
    return splitRuleType == SplitRuleType.PERCENTAGE;
  }

  public boolean isExactAmountSplit() {
    return splitRuleType == SplitRuleType.EXACT_AMOUNT;
  }

  public boolean isSharesSplit() {
    return splitRuleType == SplitRuleType.SHARES;
  }
}
