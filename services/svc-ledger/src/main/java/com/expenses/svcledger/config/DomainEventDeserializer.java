package com.expenses.svcledger.config;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.common.event.GroupEvent;
import com.expenses.common.event.SettlementEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
    // Register JavaTimeModule to handle Java 8 date/time types
    this.objectMapper.registerModule(new JavaTimeModule());
    // Configure to handle numeric timestamps as epoch seconds
    this.objectMapper
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    // Ignore unknown properties (like "topicName") to prevent deserialization
    // failures
    this.objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        false);

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

      // Deserialize based on event type using concrete classes
      if (eventType.startsWith("EXPENSE_")) {
        switch (eventType) {
          case "EXPENSE_CREATED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseCreated.class);
          }
          case "EXPENSE_UPDATED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseUpdated.class);
          }
          case "EXPENSE_DELETED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseDeleted.class);
          }
          case "EXPENSE_PARTICIPANT_ADDED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseParticipantAdded.class);
          }
          case "EXPENSE_PARTICIPANT_REMOVED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseParticipantRemoved.class);
          }
          case "EXPENSE_SPLITS_CALCULATED" -> {
            return objectMapper.readValue(json, ExpenseEvent.ExpenseSplitsCalculated.class);
          }
          default -> {
            log.warn("Unknown expense event type: {}", eventType);
            return objectMapper.readValue(json, ExpenseEvent.ExpenseCreated.class);
          }
        }
      } else if (eventType.startsWith("GROUP_") || eventType.startsWith("MEMBER_")) {
        switch (eventType) {
          case "GROUP_CREATED" -> {
            return objectMapper.readValue(json, GroupEvent.GroupCreated.class);
          }
          case "GROUP_UPDATED" -> {
            return objectMapper.readValue(json, GroupEvent.GroupUpdated.class);
          }
          case "GROUP_DELETED" -> {
            return objectMapper.readValue(json, GroupEvent.GroupDeleted.class);
          }
          case "MEMBER_ADDED" -> {
            return objectMapper.readValue(json, GroupEvent.MemberAdded.class);
          }
          case "MEMBER_REMOVED" -> {
            return objectMapper.readValue(json, GroupEvent.MemberRemoved.class);
          }
          case "MEMBER_ROLE_UPDATED" -> {
            return objectMapper.readValue(json, GroupEvent.MemberRoleUpdated.class);
          }
          case "OWNERSHIP_TRANSFERRED" -> {
            return objectMapper.readValue(json, GroupEvent.OwnershipTransferred.class);
          }
          case "GROUP_SETTINGS_UPDATED" -> {
            return objectMapper.readValue(json, GroupEvent.GroupSettingsUpdated.class);
          }
          default -> {
            log.warn("Unknown group event type: {}", eventType);
            return objectMapper.readValue(json, GroupEvent.GroupCreated.class);
          }
        }
      } else if (eventType.startsWith("PROPOSAL_") || eventType.startsWith("PAYMENT_")) {
        switch (eventType) {
          case "PROPOSAL_CREATED" -> {
            return objectMapper.readValue(json, SettlementEvent.ProposalCreated.class);
          }
          case "PROPOSAL_ACCEPTED" -> {
            return objectMapper.readValue(json, SettlementEvent.ProposalAccepted.class);
          }
          case "PROPOSAL_REJECTED" -> {
            return objectMapper.readValue(json, SettlementEvent.ProposalRejected.class);
          }
          case "PROPOSAL_CANCELLED" -> {
            return objectMapper.readValue(json, SettlementEvent.ProposalCancelled.class);
          }
          case "PROPOSAL_COMPLETED" -> {
            return objectMapper.readValue(json, SettlementEvent.ProposalCompleted.class);
          }
          case "PAYMENT_CONFIRMED" -> {
            return objectMapper.readValue(json, SettlementEvent.PaymentConfirmed.class);
          }
          case "PAYMENT_COMPLETED" -> {
            return objectMapper.readValue(json, SettlementEvent.PaymentCompleted.class);
          }
          case "PAYMENT_DISPUTED" -> {
            return objectMapper.readValue(json, SettlementEvent.PaymentDisputed.class);
          }
          default -> {
            log.warn("Unknown settlement event type: {}", eventType);
            return objectMapper.readValue(json, SettlementEvent.PaymentCompleted.class);
          }
        }
      } else {
        log.warn("Unknown event type: {}", eventType);
        // Fallback to base DomainEvent - this might not work due to abstract class
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
