package com.expenses.svcsplitengine.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Health indicator for Kafka consumers.
 * Monitors consumer status and partition assignments.
 */
@Component("kafkaConsumer")
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerHealthIndicator implements HealthIndicator {

  private final KafkaListenerEndpointRegistry registry;

  @Override
  public Health health() {
    try {
      Map<String, Object> details = new HashMap<>();
      boolean allRunning = true;
      int totalContainers = 0;
      int runningContainers = 0;

      for (MessageListenerContainer container : registry.getAllListenerContainers()) {
        totalContainers++;
        String listenerId = container.getListenerId();
        boolean isRunning = container.isRunning();

        if (isRunning) {
          runningContainers++;
        } else {
          allRunning = false;
        }

        details.put(listenerId + ".running", isRunning);

        // Get assigned partitions count
        if (container.getAssignedPartitions() != null) {
          int partitionCount = container.getAssignedPartitions().size();
          details.put(listenerId + ".partitions", partitionCount);
        }
      }

      details.put("total.containers", totalContainers);
      details.put("running.containers", runningContainers);

      if (allRunning && totalContainers > 0) {
        return Health.up()
            .withDetail("status", "All Kafka consumers are running")
            .withDetails(details)
            .build();
      } else if (runningContainers > 0) {
        return Health.up()
            .withDetail("status", String.format("%d/%d consumers running", runningContainers, totalContainers))
            .withDetails(details)
            .build();
      } else {
        return Health.down()
            .withDetail("status", "No Kafka consumers are running")
            .withDetails(details)
            .build();
      }

    } catch (Exception e) {
      log.error("Failed to check Kafka consumer health", e);
      return Health.down()
          .withDetail("error", e.getMessage())
          .build();
    }
  }
}
