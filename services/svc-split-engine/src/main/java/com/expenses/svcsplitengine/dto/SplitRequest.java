package com.expenses.svcsplitengine.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.expenses.svcsplitengine.entity.SplitCalculation;

import lombok.Builder;

/**
 * Request for calculating expense splits
 */
@Builder
public record SplitRequest(
    @NotNull(message = "Expense ID is required") UUID expenseId,

    @NotNull(message = "Group ID is required") UUID groupId,

    @NotNull(message = "Total amount is required") @Positive(message = "Total amount must be positive") Long totalAmountCents,

    @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be 3 characters") String currency,

    @NotNull(message = "Split method is required") SplitCalculation.SplitMethod splitMethod,

    @NotEmpty(message = "At least one participant is required") @Valid List<ParticipantSplitRequest> participants) {

  /**
   * Individual participant split request
   */
  @Builder
  public static record ParticipantSplitRequest(
      @NotNull(message = "User ID is required") UUID userId,

      @DecimalMin(value = "0.0", message = "Split value must be non-negative") @DecimalMax(value = "999999999.99", message = "Split value too large") BigDecimal splitValue // percentage,
                                                                                                                                                                            // exact
                                                                                                                                                                            // amount,
                                                                                                                                                                            // or
                                                                                                                                                                            // share
                                                                                                                                                                            // count
  ) {
  }

  /**
   * Create equal split request
   */
  public static SplitRequest createEqualSplit(UUID expenseId, UUID groupId,
      Long totalAmountCents, String currency,
      List<UUID> participantIds) {
    List<ParticipantSplitRequest> participants = participantIds.stream()
        .map(id -> ParticipantSplitRequest.builder()
            .userId(id)
            .splitValue(BigDecimal.ONE)
            .build())
        .toList();

    return SplitRequest.builder()
        .expenseId(expenseId)
        .groupId(groupId)
        .totalAmountCents(totalAmountCents)
        .currency(currency)
        .splitMethod(SplitCalculation.SplitMethod.EQUAL)
        .participants(participants)
        .build();
  }

  /**
   * Create percentage split request
   */
  public static SplitRequest createPercentageSplit(UUID expenseId, UUID groupId,
      Long totalAmountCents, String currency,
      List<ParticipantSplitRequest> participants) {
    return SplitRequest.builder()
        .expenseId(expenseId)
        .groupId(groupId)
        .totalAmountCents(totalAmountCents)
        .currency(currency)
        .splitMethod(SplitCalculation.SplitMethod.PERCENTAGE)
        .participants(participants)
        .build();
  }

  /**
   * Create exact amounts split request
   */
  public static SplitRequest createExactAmountsSplit(UUID expenseId, UUID groupId,
      Long totalAmountCents, String currency,
      List<ParticipantSplitRequest> participants) {
    return SplitRequest.builder()
        .expenseId(expenseId)
        .groupId(groupId)
        .totalAmountCents(totalAmountCents)
        .currency(currency)
        .splitMethod(SplitCalculation.SplitMethod.EXACT_AMOUNTS)
        .participants(participants)
        .build();
  }

  /**
   * Create shares split request
   */
  public static SplitRequest createSharesSplit(UUID expenseId, UUID groupId,
      Long totalAmountCents, String currency,
      List<ParticipantSplitRequest> participants) {
    return SplitRequest.builder()
        .expenseId(expenseId)
        .groupId(groupId)
        .totalAmountCents(totalAmountCents)
        .currency(currency)
        .splitMethod(SplitCalculation.SplitMethod.SHARES)
        .participants(participants)
        .build();
  }
}
