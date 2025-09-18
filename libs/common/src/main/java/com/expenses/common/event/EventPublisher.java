package com.expenses.common.event;

import java.util.concurrent.CompletableFuture;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

  private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

  /**
   * Publishes a domain event to Kafka
   */
  public CompletableFuture<SendResult<String, DomainEvent>> publishEvent(DomainEvent event) {
    String key = generateEventKey(event);
    String topic = event.getTopicName();

    log.info("Publishing event: {} to topic: {} with key: {}",
        event.getEventName(), topic, key);

    return kafkaTemplate.send(topic, key, event)
        .whenComplete((result, ex) -> {
          if (ex == null) {
            log.debug("Event published successfully: {} to partition: {} with offset: {}",
                event.getEventName(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
          } else {
            log.error("Failed to publish event: {} to topic: {}",
                event.getEventName(), topic, ex);
          }
        });
  }

  /**
   * Publishes event asynchronously without waiting for result
   */
  public void publishEventAsync(DomainEvent event) {
    publishEvent(event);
  }

  /**
   * Publishes event synchronously and waits for result
   */
  public void publishEventSync(DomainEvent event) {
    try {
      publishEvent(event).get();
      log.info("Event published synchronously: {}", event.getEventName());
    } catch (Exception e) {
      log.error("Failed to publish event synchronously: {}", event.getEventName(), e);
      throw new RuntimeException("Failed to publish event: " + event.getEventName(), e);
    }
  }

  /**
   * Generates partition key for the event
   * Events related to same aggregate should go to same partition for ordering
   */
  private String generateEventKey(DomainEvent event) {
    if (event.getAggregateId() != null) {
      return event.getAggregateType() + ":" + event.getAggregateId();
    }
    return event.getEventType() + ":" + event.getEventId();
  }
}
