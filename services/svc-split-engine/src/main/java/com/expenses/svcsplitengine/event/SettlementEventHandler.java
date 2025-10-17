package com.expenses.svcsplitengine.event;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.SettlementEvent;
import com.expenses.svcsplitengine.service.BalanceUpdateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event handler for settlement events.
 * Updates user balances when settlement payments are completed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventHandler extends BaseEventHandler {

  private final BalanceUpdateService balanceUpdateService;

  @org.springframework.kafka.annotation.KafkaListener(topics = "settlement-events", groupId = "split-engine-service")
  public void handleSettlementEvents(ConsumerRecord<String, DomainEvent> record, Acknowledgment acknowledgment) {
    DomainEvent event = record.value();
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();

    handleEvent(event, topic, partition, offset, record, acknowledgment);
  }

  @Override
  protected void processEvent(DomainEvent event) throws Exception {
    if (event instanceof SettlementEvent settlementEvent) {
      switch (settlementEvent.getEventName()) {
        case "PAYMENT_COMPLETED" -> handlePaymentCompleted((SettlementEvent.PaymentCompleted) settlementEvent);
        case "PROPOSAL_CREATED" -> handleProposalCreated((SettlementEvent.ProposalCreated) settlementEvent);
        case "PROPOSAL_COMPLETED" -> handleProposalCompleted((SettlementEvent.ProposalCompleted) settlementEvent);
        default -> log.debug("Unhandled settlement event: {}", settlementEvent.getEventName());
      }
    }
  }

  /**
   * Handle payment completed - adjust balances to reflect the settlement
   * When a payment is completed:
   * - The payer's negative balance should decrease (or positive balance increase)
   * - The payee's positive balance should decrease (or negative balance increase)
   */
  private void handlePaymentCompleted(SettlementEvent.PaymentCompleted event) {
    log.info("Processing payment completed event for payment: {} in group: {}",
        event.getPaymentId(), event.getGroupId());

    try {
      UUID groupId = event.getGroupId();
      UUID payerId = event.getPayerId();
      UUID payeeId = event.getPayeeId();
      Long amountCents = event.getAmountCents();
      String currency = event.getCurrency();

      // Validate that the amount is positive
      if (amountCents == null || amountCents <= 0) {
        log.warn("Invalid settlement amount: {} cents for payment {}", amountCents, event.getPaymentId());
        return;
      }

      // Apply settlement payment to balances
      // The payer sends money, so their balance improves (increases if negative,
      // decreases if positive)
      // The payee receives money, so their balance worsens (decreases if positive,
      // increases if negative)
      balanceUpdateService.applySettlementPayment(
          groupId,
          payerId,
          payeeId,
          amountCents,
          currency,
          event.getPaymentId());

      log.info("Successfully processed payment completed event for payment {} - {} {} from {} to {}",
          event.getPaymentId(),
          amountCents / 100.0,
          currency,
          payerId,
          payeeId);

    } catch (Exception e) {
      log.error("Failed to process payment completed event for payment {}: {}",
          event.getPaymentId(), e.getMessage(), e);
      throw e; // Rethrow to trigger retry
    }
  }

  /**
   * Handle proposal created - just log for now, no balance changes until payments
   * complete
   */
  private void handleProposalCreated(SettlementEvent.ProposalCreated event) {
    log.info("Settlement proposal created: {} in group: {} with {} payments",
        event.getProposalId(), event.getGroupId(),
        event.getPayments() != null ? event.getPayments().size() : 0);
  }

  /**
   * Handle proposal completed - log completion for audit purposes
   */
  private void handleProposalCompleted(SettlementEvent.ProposalCompleted event) {
    log.info("Settlement proposal completed: {} in group: {} - total amount: {} {}, {} payments",
        event.getProposalId(),
        event.getGroupId(),
        event.getTotalAmountCents() / 100.0,
        event.getCurrency(),
        event.getPaymentCount());
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof SettlementEvent;
  }
}

