package com.expenses.common.event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserEvent.class, name = "USER"),
    @JsonSubTypes.Type(value = GroupEvent.class, name = "GROUP"),
    @JsonSubTypes.Type(value = ExpenseEvent.class, name = "EXPENSE"),
    @JsonSubTypes.Type(value = LedgerEvent.class, name = "LEDGER")
})
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class DomainEvent {

  private UUID eventId = UUID.randomUUID();
  private ZonedDateTime timestamp = ZonedDateTime.now();
  private String eventType;
  private String eventName;
  private UUID aggregateId;
  private String aggregateType;
  private Integer version;
  private UUID causedBy; // User who caused this event
  private UUID correlationId; // For tracing related events

  protected DomainEvent(String eventType, String eventName, UUID aggregateId, String aggregateType) {
    this.eventType = eventType;
    this.eventName = eventName;
    this.aggregateId = aggregateId;
    this.aggregateType = aggregateType;
    this.version = 1;
  }

  public abstract String getTopicName();
}
