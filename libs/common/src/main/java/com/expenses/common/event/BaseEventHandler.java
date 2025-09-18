package com.expenses.common.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for event handlers with common functionality
 */
@Slf4j
public abstract class BaseEventHandler {

  /**
   * Handles incoming events with error handling and logging
   */
  protected void handleEvent(@Payload DomainEvent event,
      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
      @Header(KafkaHeaders.OFFSET) long offset,
      ConsumerRecord<String, DomainEvent> record,
      Acknowledgment acknowledgment) {

    log.info("Received event: {} from topic: {} partition: {} offset: {}",
        event.getEventName(), topic, partition, offset);

    try {
      // Idempotency check
      if (shouldSkipEvent(event)) {
        log.info("Skipping duplicate event: {} with ID: {}",
            event.getEventName(), event.getEventId());
        if (acknowledgment != null) {
          acknowledgment.acknowledge();
        }
        return;
      }

      // Process the event
      processEvent(event);

      // Mark as processed
      markEventAsProcessed(event);

      // Acknowledge the message
      if (acknowledgment != null) {
        acknowledgment.acknowledge();
      }

      log.debug("Successfully processed event: {} with ID: {}",
          event.getEventName(), event.getEventId());

    } catch (Exception e) {
      log.error("Error processing event: {} with ID: {}",
          event.getEventName(), event.getEventId(), e);

      handleError(event, e, acknowledgment);
    }
  }

  /**
   * Abstract method to be implemented by concrete handlers
   */
  protected abstract void processEvent(DomainEvent event) throws Exception;

  /**
   * Check if event should be skipped (idempotency)
   */
  protected boolean shouldSkipEvent(DomainEvent event) {
    // Default implementation - can be overridden
    return false;
  }

  /**
   * Mark event as processed for idempotency
   */
  protected void markEventAsProcessed(DomainEvent event) {
    // Default implementation - can be overridden
    // Could store in Redis/Database for idempotency tracking
  }

  /**
   * Handle errors during event processing
   */
  protected void handleError(DomainEvent event, Exception error, Acknowledgment acknowledgment) {
    // Default error handling - log and acknowledge to avoid infinite retry
    log.error("Failed to process event: {} - acknowledging to avoid infinite retry",
        event.getEventName());

    if (acknowledgment != null) {
      acknowledgment.acknowledge();
    }

    // Could implement retry logic, DLQ, or alerting here
  }

  /**
   * Check if this handler supports the given event type
   */
  protected boolean canHandle(DomainEvent event) {
    return true; // Default implementation accepts all events
  }
}
