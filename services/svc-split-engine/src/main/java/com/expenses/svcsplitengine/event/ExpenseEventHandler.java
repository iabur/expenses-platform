package com.expenses.svcsplitengine.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.EventHandler;
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

  @EventHandler(topics = "expense-events", groupId = "split-engine-service", eventTypes = {
      ExpenseEvent.ExpenseCreated.class, ExpenseEvent.ExpenseSplitsCalculated.class })
  public void handleExpenseEvents(@Payload DomainEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset,
      ConsumerRecord<String, DomainEvent> record,
      Acknowledgment acknowledgment) {

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

        // Calculate splits
        splitCalculationService.calculateSplits(splitRequest);

        log.info("Successfully triggered split calculation for expense: {}",
            event.getAggregateId());

      } catch (Exception e) {
        log.error("Failed to calculate splits for expense {}: {}",
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
   */
  private void handleExpenseSplitsCalculated(ExpenseEvent.ExpenseSplitsCalculated event) {
    log.info("Processing expense splits calculated event for expense: {}",
        event.getAggregateId());

    try {
      // Update group balances based on calculated splits
      balanceUpdateService.updateBalancesFromSplits(
          event.getGroupId(),
          event.getAggregateId(),
          event.getSplits(),
          event.getCurrency());

      log.info("Successfully updated balances for expense: {}", event.getAggregateId());

    } catch (Exception e) {
      log.error("Failed to update balances for expense {}: {}",
          event.getAggregateId(), e.getMessage(), e);
      throw e; // Rethrow to trigger retry/DLQ
    }
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
