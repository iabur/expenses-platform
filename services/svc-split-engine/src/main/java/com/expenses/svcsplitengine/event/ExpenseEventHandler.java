package com.expenses.svcsplitengine.event;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcsplitengine.dto.SplitRequest;
import com.expenses.svcsplitengine.service.BalanceUpdateService;
import com.expenses.svcsplitengine.service.SplitCalculationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventHandler extends BaseEventHandler {

  private final SplitCalculationService splitCalculationService;
  private final BalanceUpdateService balanceUpdateService;

  @org.springframework.kafka.annotation.KafkaListener(topics = "expense-events", groupId = "split-engine-service")
  public void handleExpenseEvents(ConsumerRecord<String, DomainEvent> record, Acknowledgment acknowledgment) {
    DomainEvent event = record.value();
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();

    handleEvent(event, topic, partition, offset, record, acknowledgment);
  }

  @Override
  protected void processEvent(DomainEvent event) throws Exception {
    if (event instanceof ExpenseEvent expenseEvent) {
      switch (expenseEvent.getEventName()) {
        case "EXPENSE_CREATED" -> handleExpenseCreated((ExpenseEvent.ExpenseCreated) expenseEvent);
        case "EXPENSE_SPLITS_CALCULATED" ->
          handleExpenseSplitsCalculated((ExpenseEvent.ExpenseSplitsCalculated) expenseEvent);
        default -> log.debug("Unhandled expense event: {}", expenseEvent.getEventName());
      }
    }
  }

  /**
   * Handle expense created - trigger split calculation if participants provided
   */
  private void handleExpenseCreated(ExpenseEvent.ExpenseCreated event) {
    log.info("Processing expense created event for expense: {}", event.getAggregateId());

    // Check if expense has participants for splitting
    if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
      try {
        // Create split request from expense event
        SplitRequest splitRequest = createSplitRequestFromExpense(event);

        // Calculate splits and immediately update balances with the correct payer
        // We do this here because we have access to the original expense creator
        UUID paidBy = event.getCreatedBy();
        log.info("Expense {} was paid by: {}", event.getAggregateId(), paidBy);

        // Calculate splits
        var splitResult = splitCalculationService.calculateSplits(splitRequest);

        // Convert SplitResult to SplitInfo list for balance update
        List<com.expenses.common.event.ExpenseEvent.SplitInfo> splits = splitResult.participantSplits().stream()
            .map(ps -> com.expenses.common.event.ExpenseEvent.SplitInfo.builder()
                .userId(ps.userId())
                .amountCents(ps.calculatedAmountCents())
                .currency(ps.currency())
                .build())
            .collect(java.util.stream.Collectors.toList());

        // Update balances immediately with the correct payer
        balanceUpdateService.updateBalancesFromExpense(
            event.getGroupId(),
            event.getAggregateId(),
            paidBy,
            splits,
            event.getCurrency());

        log.info("Successfully processed expense creation and updated balances for expense: {}",
            event.getAggregateId());

      } catch (Exception e) {
        log.error("Failed to process expense creation for expense {}: {}",
            event.getAggregateId(), e.getMessage(), e);
        // Don't rethrow - we'll handle this as a failed calculation
      }
    } else {
      log.debug("Expense {} has no participants, skipping split calculation",
          event.getAggregateId());
    }
  }

  /**
   * Handle expense splits calculated - update group balances
   * NOTE: We now handle balance updates directly in handleExpenseCreated
   * to ensure we have access to the correct payer information
   */
  private void handleExpenseSplitsCalculated(ExpenseEvent.ExpenseSplitsCalculated event) {
    log.info("Received expense splits calculated event for expense: {} - skipping as balances already updated",
        event.getAggregateId());

    // We no longer process this event because we handle everything in
    // handleExpenseCreated
    // where we have access to the original expense creator (payer) information
  }

  /**
   * Create split request from expense created event
   */
  private SplitRequest createSplitRequestFromExpense(ExpenseEvent.ExpenseCreated event) {
    // Convert participants to split request format
    var participants = event.getParticipants().stream()
        .map(p -> SplitRequest.ParticipantSplitRequest.builder()
            .userId(p.getUserId())
            .splitValue(p.getSplitRuleValue())
            .build())
        .toList();

    // Determine split method from participant rules
    var splitMethod = determineSplitMethod(event.getParticipants());

    return SplitRequest.builder()
        .expenseId(event.getAggregateId())
        .groupId(event.getGroupId())
        .totalAmountCents(event.getAmountCents())
        .currency(event.getCurrency())
        .splitMethod(splitMethod)
        .participants(participants)
        .build();
  }

  /**
   * Determine split method from participant rules
   */
  private com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod determineSplitMethod(
      java.util.List<ExpenseEvent.ParticipantInfo> participants) {

    if (participants.isEmpty()) {
      return com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod.EQUAL;
    }

    // Check first participant's split rule type to determine method
    var firstParticipant = participants.get(0);
    String ruleType = firstParticipant.getSplitRuleType();

    return switch (ruleType.toUpperCase()) {
      case "PERCENTAGE" -> com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod.PERCENTAGE;
      case "EXACT_AMOUNT" -> com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod.EXACT_AMOUNTS;
      case "SHARES" -> com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod.SHARES;
      default -> com.expenses.svcsplitengine.entity.SplitCalculation.SplitMethod.EQUAL;
    };
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof ExpenseEvent;
  }
}
