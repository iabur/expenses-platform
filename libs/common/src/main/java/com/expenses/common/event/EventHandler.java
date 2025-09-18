package com.expenses.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;

/**
 * Annotation to mark methods as event handlers
 * Combines @KafkaListener with additional metadata
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@KafkaListener
public @interface EventHandler {

  /**
   * Kafka topics to listen to
   */
  String[] topics();

  /**
   * Consumer group ID
   */
  String groupId();

  /**
   * Event types this handler can process
   */
  Class<? extends DomainEvent>[] eventTypes() default {};

  /**
   * Whether to enable automatic offset commit
   */
  String autoCommit() default "true";

  /**
   * Error handling strategy
   */
  ErrorStrategy errorStrategy() default ErrorStrategy.LOG_AND_CONTINUE;

  enum ErrorStrategy {
    LOG_AND_CONTINUE,
    RETRY_AND_DLQ,
    FAIL_FAST
  }
}
