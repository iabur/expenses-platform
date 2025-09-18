package com.expenses.svcsplitengine.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.expenses.svcsplitengine.entity.SplitCalculation;

import lombok.Builder;

/**
 * Result of expense split calculation
 */
@Builder
public record SplitResult(
    UUID calculationId,
    UUID expenseId,
    UUID groupId,
    Long totalAmountCents,
    String currency,
    SplitCalculation.SplitMethod splitMethod,
    SplitCalculation.CalculationStatus status,
    ZonedDateTime calculatedAt,
    String errorMessage,
    List<ParticipantSplitResult> participantSplits) {

  /**
   * Individual participant split result
   */
  @Builder
  public static record ParticipantSplitResult(
      UUID participantId,
      UUID userId,
      String splitRuleType,
      BigDecimal splitRuleValue,
      Long calculatedAmountCents,
      String currency) {

    public BigDecimal getCalculatedAmountDecimal() {
      return calculatedAmountCents != null ? BigDecimal.valueOf(calculatedAmountCents).divide(BigDecimal.valueOf(100))
          : BigDecimal.ZERO;
    }
  }

  /**
   * Create from SplitCalculation entity
   */
  public static SplitResult from(SplitCalculation calculation) {
    List<ParticipantSplitResult> participantSplits = calculation.getParticipantSplits()
        .stream()
        .map(split -> ParticipantSplitResult.builder()
            .participantId(split.getId())
            .userId(split.getUserId())
            .splitRuleType(split.getSplitRuleType().name())
            .splitRuleValue(split.getSplitRuleValue())
            .calculatedAmountCents(split.getCalculatedAmountCents())
            .currency(split.getCurrency())
            .build())
        .toList();

    return SplitResult.builder()
        .calculationId(calculation.getId())
        .expenseId(calculation.getExpenseId())
        .groupId(calculation.getGroupId())
        .totalAmountCents(calculation.getTotalAmountCents())
        .currency(calculation.getCurrency())
        .splitMethod(calculation.getSplitMethod())
        .status(calculation.getCalculationStatus())
        .calculatedAt(calculation.getCalculatedAt())
        .errorMessage(calculation.getErrorMessage())
        .participantSplits(participantSplits)
        .build();
  }

  /**
   * Get total calculated amount as decimal
   */
  public BigDecimal getTotalAmountDecimal() {
    return totalAmountCents != null ? BigDecimal.valueOf(totalAmountCents).divide(BigDecimal.valueOf(100))
        : BigDecimal.ZERO;
  }

  /**
   * Check if calculation was successful
   */
  public boolean isSuccessful() {
    return status == SplitCalculation.CalculationStatus.CALCULATED;
  }

  /**
   * Check if calculation failed
   */
  public boolean hasFailed() {
    return status == SplitCalculation.CalculationStatus.FAILED;
  }

  /**
   * Check if calculation is pending
   */
  public boolean isPending() {
    return status == SplitCalculation.CalculationStatus.PENDING;
  }

  /**
   * Get participant split for specific user
   */
  public ParticipantSplitResult getParticipantSplit(UUID userId) {
    return participantSplits.stream()
        .filter(split -> split.userId().equals(userId))
        .findFirst()
        .orElse(null);
  }

  /**
   * Get summary of split calculation
   */
  public String getSummary() {
    if (hasFailed()) {
      return String.format("Split calculation failed: %s", errorMessage);
    }

    if (isPending()) {
      return "Split calculation is pending";
    }

    return String.format("Split %.2f %s among %d participants using %s method",
        getTotalAmountDecimal(), currency, participantSplits.size(),
        splitMethod.name().toLowerCase().replace('_', ' '));
  }
}
