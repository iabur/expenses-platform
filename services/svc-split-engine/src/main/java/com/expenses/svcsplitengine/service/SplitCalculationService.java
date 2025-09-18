package com.expenses.svcsplitengine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.EventPublisher;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcsplitengine.dto.SplitRequest;
import com.expenses.svcsplitengine.dto.SplitResult;
import com.expenses.svcsplitengine.entity.ParticipantSplit;
import com.expenses.svcsplitengine.entity.SplitCalculation;
import com.expenses.svcsplitengine.exception.InvalidSplitException;
import com.expenses.svcsplitengine.repository.SplitCalculationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SplitCalculationService {

  private final SplitCalculationRepository splitCalculationRepository;
  private final EventPublisher eventPublisher;

  /**
   * Calculate expense splits based on split request
   */
  public SplitResult calculateSplits(SplitRequest request) {
    log.info("Calculating splits for expense {} with method {}",
        request.expenseId(), request.splitMethod());

    try {
      // Create split calculation record
      SplitCalculation calculation = SplitCalculation.builder()
          .expenseId(request.expenseId())
          .groupId(request.groupId())
          .totalAmountCents(request.totalAmountCents())
          .currency(request.currency())
          .splitMethod(request.splitMethod())
          .build();

      // Calculate individual splits based on method
      List<ParticipantSplit> splits = calculateParticipantSplits(calculation, request);

      // Validate total adds up correctly
      validateSplitTotal(splits, request.totalAmountCents());

      // Save calculation with splits
      splits.forEach(calculation::addParticipantSplit);
      calculation.markAsCalculated();

      SplitCalculation savedCalculation = splitCalculationRepository.save(calculation);

      // Publish splits calculated event
      publishSplitsCalculatedEvent(savedCalculation);

      log.info("Successfully calculated {} splits for expense {}",
          splits.size(), request.expenseId());

      return SplitResult.from(savedCalculation);

    } catch (Exception e) {
      log.error("Failed to calculate splits for expense {}: {}",
          request.expenseId(), e.getMessage(), e);

      // Save failed calculation
      SplitCalculation failedCalculation = SplitCalculation.builder()
          .expenseId(request.expenseId())
          .groupId(request.groupId())
          .totalAmountCents(request.totalAmountCents())
          .currency(request.currency())
          .splitMethod(request.splitMethod())
          .build();
      failedCalculation.markAsFailed(e.getMessage());
      splitCalculationRepository.save(failedCalculation);

      throw new InvalidSplitException("Failed to calculate splits: " + e.getMessage(), e);
    }
  }

  /**
   * Calculate individual participant splits based on split method
   */
  private List<ParticipantSplit> calculateParticipantSplits(SplitCalculation calculation,
      SplitRequest request) {
    return switch (request.splitMethod()) {
      case EQUAL -> calculateEqualSplits(calculation, request);
      case PERCENTAGE -> calculatePercentageSplits(calculation, request);
      case EXACT_AMOUNTS -> calculateExactAmountSplits(calculation, request);
      case SHARES -> calculateSharesSplits(calculation, request);
    };
  }

  /**
   * Equal split among all participants
   */
  private List<ParticipantSplit> calculateEqualSplits(SplitCalculation calculation,
      SplitRequest request) {
    List<ParticipantSplit> splits = new ArrayList<>();
    int participantCount = request.participants().size();

    if (participantCount == 0) {
      throw new InvalidSplitException("No participants provided for equal split");
    }

    BigDecimal totalAmount = BigDecimal.valueOf(request.totalAmountCents())
        .divide(BigDecimal.valueOf(100));
    BigDecimal equalShare = totalAmount.divide(BigDecimal.valueOf(participantCount),
        2, RoundingMode.DOWN);

    // Calculate equal shares
    Long equalShareCents = equalShare.multiply(BigDecimal.valueOf(100)).longValue();
    Long totalDistributed = 0L;

    for (int i = 0; i < request.participants().size(); i++) {
      var participant = request.participants().get(i);
      Long shareAmount = equalShareCents;

      // Add remainder to last participant to ensure total matches
      if (i == request.participants().size() - 1) {
        shareAmount = request.totalAmountCents() - totalDistributed;
      }

      ParticipantSplit split = ParticipantSplit.builder()
          .splitCalculation(calculation)
          .userId(participant.userId())
          .splitRuleType(ParticipantSplit.SplitRuleType.EQUAL)
          .splitRuleValue(BigDecimal.ONE) // Equal share = 1 unit
          .calculatedAmountCents(shareAmount)
          .currency(request.currency())
          .build();

      splits.add(split);
      totalDistributed += shareAmount;
    }

    return splits;
  }

  /**
   * Percentage-based splits
   */
  private List<ParticipantSplit> calculatePercentageSplits(SplitCalculation calculation,
      SplitRequest request) {
    List<ParticipantSplit> splits = new ArrayList<>();

    // Validate percentages add up to 100
    BigDecimal totalPercentage = request.participants().stream()
        .map(p -> p.splitValue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalPercentage.compareTo(BigDecimal.valueOf(100)) != 0) {
      throw new InvalidSplitException(
          String.format("Percentages must add up to 100%%, got %.2f%%", totalPercentage));
    }

    BigDecimal totalAmount = BigDecimal.valueOf(request.totalAmountCents())
        .divide(BigDecimal.valueOf(100));
    Long totalDistributed = 0L;

    for (int i = 0; i < request.participants().size(); i++) {
      var participant = request.participants().get(i);

      Long shareAmount;
      if (i == request.participants().size() - 1) {
        // Last participant gets remainder to ensure total matches
        shareAmount = request.totalAmountCents() - totalDistributed;
      } else {
        BigDecimal percentage = participant.splitValue().divide(BigDecimal.valueOf(100));
        BigDecimal shareAmountDecimal = totalAmount.multiply(percentage);
        shareAmount = shareAmountDecimal.multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.DOWN)
            .longValue();
      }

      ParticipantSplit split = ParticipantSplit.builder()
          .splitCalculation(calculation)
          .userId(participant.userId())
          .splitRuleType(ParticipantSplit.SplitRuleType.PERCENTAGE)
          .splitRuleValue(participant.splitValue())
          .calculatedAmountCents(shareAmount)
          .currency(request.currency())
          .build();

      splits.add(split);
      totalDistributed += shareAmount;
    }

    return splits;
  }

  /**
   * Exact amount splits
   */
  private List<ParticipantSplit> calculateExactAmountSplits(SplitCalculation calculation,
      SplitRequest request) {
    List<ParticipantSplit> splits = new ArrayList<>();

    // Validate exact amounts add up to total
    Long totalExactAmounts = request.participants().stream()
        .mapToLong(p -> p.splitValue().multiply(BigDecimal.valueOf(100)).longValue())
        .sum();

    if (!totalExactAmounts.equals(request.totalAmountCents())) {
      throw new InvalidSplitException(
          String.format("Exact amounts must add up to total expense amount. " +
              "Expected: %.2f, Got: %.2f",
              request.totalAmountCents() / 100.0,
              totalExactAmounts / 100.0));
    }

    for (var participant : request.participants()) {
      Long exactAmountCents = participant.splitValue()
          .multiply(BigDecimal.valueOf(100))
          .longValue();

      ParticipantSplit split = ParticipantSplit.builder()
          .splitCalculation(calculation)
          .userId(participant.userId())
          .splitRuleType(ParticipantSplit.SplitRuleType.EXACT_AMOUNT)
          .splitRuleValue(participant.splitValue())
          .calculatedAmountCents(exactAmountCents)
          .currency(request.currency())
          .build();

      splits.add(split);
    }

    return splits;
  }

  /**
   * Share-based splits (e.g., 2 shares, 3 shares, 1 share)
   */
  private List<ParticipantSplit> calculateSharesSplits(SplitCalculation calculation,
      SplitRequest request) {
    List<ParticipantSplit> splits = new ArrayList<>();

    // Calculate total shares
    BigDecimal totalShares = request.participants().stream()
        .map(p -> p.splitValue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidSplitException("Total shares must be greater than zero");
    }

    BigDecimal totalAmount = BigDecimal.valueOf(request.totalAmountCents())
        .divide(BigDecimal.valueOf(100));
    BigDecimal perShareAmount = totalAmount.divide(totalShares, 10, RoundingMode.DOWN);
    Long totalDistributed = 0L;

    for (int i = 0; i < request.participants().size(); i++) {
      var participant = request.participants().get(i);

      Long shareAmount;
      if (i == request.participants().size() - 1) {
        // Last participant gets remainder
        shareAmount = request.totalAmountCents() - totalDistributed;
      } else {
        BigDecimal participantAmount = perShareAmount.multiply(participant.splitValue());
        shareAmount = participantAmount.multiply(BigDecimal.valueOf(100))
            .setScale(0, RoundingMode.DOWN)
            .longValue();
      }

      ParticipantSplit split = ParticipantSplit.builder()
          .splitCalculation(calculation)
          .userId(participant.userId())
          .splitRuleType(ParticipantSplit.SplitRuleType.SHARES)
          .splitRuleValue(participant.splitValue())
          .calculatedAmountCents(shareAmount)
          .currency(request.currency())
          .build();

      splits.add(split);
      totalDistributed += shareAmount;
    }

    return splits;
  }

  /**
   * Validate that calculated splits add up to total expense amount
   */
  private void validateSplitTotal(List<ParticipantSplit> splits, Long expectedTotal) {
    Long calculatedTotal = splits.stream()
        .mapToLong(ParticipantSplit::getCalculatedAmountCents)
        .sum();

    if (!calculatedTotal.equals(expectedTotal)) {
      throw new InvalidSplitException(
          String.format("Split calculation error: calculated total (%.2f) " +
              "doesn't match expected total (%.2f)",
              calculatedTotal / 100.0, expectedTotal / 100.0));
    }
  }

  /**
   * Publish splits calculated event
   */
  private void publishSplitsCalculatedEvent(SplitCalculation calculation) {
    List<ExpenseEvent.SplitInfo> splitInfos = calculation.getParticipantSplits().stream()
        .map(split -> ExpenseEvent.SplitInfo.builder()
            .userId(split.getUserId())
            .amountCents(split.getCalculatedAmountCents())
            .currency(split.getCurrency())
            .build())
        .collect(Collectors.toList());

    ExpenseEvent.ExpenseSplitsCalculated event = new ExpenseEvent.ExpenseSplitsCalculated(
        calculation.getExpenseId(),
        calculation.getGroupId(),
        splitInfos,
        calculation.getTotalAmountCents(),
        calculation.getCurrency());

    eventPublisher.publishEventAsync(event);
  }

  /**
   * Get existing split calculation for an expense
   */
  @Transactional(readOnly = true)
  public SplitResult getSplitCalculation(UUID expenseId) {
    return splitCalculationRepository.findByExpenseIdAndCalculationStatus(
        expenseId, SplitCalculation.CalculationStatus.CALCULATED)
        .map(SplitResult::from)
        .orElse(null);
  }
}
