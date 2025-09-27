package com.expenses.svcledger.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.common.event.GroupEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom deserializer for DomainEvent that properly handles polymorphic types
 */
@Slf4j
public class DomainEventDeserializer implements Deserializer<DomainEvent> {

  private final ObjectMapper objectMapper;
  private final JsonDeserializer<DomainEvent> jsonDeserializer;

  public DomainEventDeserializer() {
    this.objectMapper = new ObjectMapper();
    this.jsonDeserializer = new JsonDeserializer<>(DomainEvent.class);

    // Configure the JSON deserializer for polymorphic types
    this.jsonDeserializer.setRemoveTypeHeaders(false);
    this.jsonDeserializer.setUseTypeMapperForKey(false);
    this.jsonDeserializer.setUseTypeHeaders(false);
  }

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    jsonDeserializer.configure(configs, isKey);
  }

  @Override
  public DomainEvent deserialize(String topic, byte[] data) {
    if (data == null) {
      return null;
    }

    try {
      String json = new String(data, StandardCharsets.UTF_8);
      log.debug("Deserializing JSON: {}", json);

      // Parse the JSON to get event type
      JsonNode jsonNode = objectMapper.readTree(json);
      String eventType = jsonNode.get("eventName").asText();
      
      // Deserialize based on event type
      if (eventType.startsWith("EXPENSE_")) {
        return objectMapper.readValue(json, ExpenseEvent.class);
      } else if (eventType.startsWith("GROUP_")) {
        return objectMapper.readValue(json, GroupEvent.class);
      } else {
        // Fallback to base DomainEvent
        return objectMapper.readValue(json, DomainEvent.class);
      }
    } catch (Exception e) {
      log.error("Failed to deserialize event: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to deserialize event", e);
    }
  }

  @Override
  public void close() {
    jsonDeserializer.close();
  }
}
