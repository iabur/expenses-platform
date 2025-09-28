package com.expenses.common.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseCreated.class, name = "EXPENSE_CREATED"),
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseUpdated.class, name = "EXPENSE_UPDATED"),
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseDeleted.class, name = "EXPENSE_DELETED"),
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseParticipantAdded.class, name = "EXPENSE_PARTICIPANT_ADDED"),
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseParticipantRemoved.class, name = "EXPENSE_PARTICIPANT_REMOVED"),
    @JsonSubTypes.Type(value = ExpenseEvent.ExpenseSplitsCalculated.class, name = "EXPENSE_SPLITS_CALCULATED")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public abstract class ExpenseEvent extends DomainEvent {

  public static final String TOPIC = "expense-events";

  protected ExpenseEvent(String eventName, UUID expenseId) {
    super("EXPENSE", eventName, expenseId, "Expense");
  }

  @Override
  public String getTopicName() {
    return TOPIC;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseCreated extends ExpenseEvent {
    private UUID groupId;
    private String currency;
    private Long amountCents;
    private LocalDate occurredAt;
    private String note;
    private String category;
    private UUID createdBy;
    private UUID paidBy;
    private List<ParticipantInfo> participants;

    public ExpenseCreated(UUID expenseId, UUID groupId, String currency, Long amountCents,
        LocalDate occurredAt, String note, String category, UUID createdBy, UUID paidBy,
        List<ParticipantInfo> participants) {
      super("EXPENSE_CREATED", expenseId);
      this.groupId = groupId;
      this.currency = currency;
      this.amountCents = amountCents;
      this.occurredAt = occurredAt;
      this.note = note;
      this.category = category;
      this.createdBy = createdBy;
      this.paidBy = paidBy;
      this.participants = participants;
      setCausedBy(createdBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseUpdated extends ExpenseEvent {
    private String currency;
    private Long amountCents;
    private LocalDate occurredAt;
    private String note;
    private String category;
    private UUID updatedBy;

    public ExpenseUpdated(UUID expenseId, String currency, Long amountCents,
        LocalDate occurredAt, String note, String category, UUID updatedBy) {
      super("EXPENSE_UPDATED", expenseId);
      this.currency = currency;
      this.amountCents = amountCents;
      this.occurredAt = occurredAt;
      this.note = note;
      this.category = category;
      this.updatedBy = updatedBy;
      setCausedBy(updatedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseDeleted extends ExpenseEvent {
    private UUID groupId;
    private UUID deletedBy;

    public ExpenseDeleted(UUID expenseId, UUID groupId, UUID deletedBy) {
      super("EXPENSE_DELETED", expenseId);
      this.groupId = groupId;
      this.deletedBy = deletedBy;
      setCausedBy(deletedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseSplitsCalculated extends ExpenseEvent {
    private UUID groupId;
    private List<SplitInfo> splits;
    private Long totalAmountCents;
    private String currency;
    private UUID paidBy;

    public ExpenseSplitsCalculated(UUID expenseId, UUID groupId, List<SplitInfo> splits,
        Long totalAmountCents, String currency, UUID paidBy, UUID causedBy) {
      super("EXPENSE_SPLITS_CALCULATED", expenseId);
      this.groupId = groupId;
      this.splits = splits;
      this.totalAmountCents = totalAmountCents;
      this.currency = currency;
      this.paidBy = paidBy;
      setCausedBy(causedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseParticipantAdded extends ExpenseEvent {
    private UUID userId;
    private String splitRuleType;
    private BigDecimal splitRuleValue;
    private UUID addedBy;

    public ExpenseParticipantAdded(UUID expenseId, UUID userId, String splitRuleType,
        BigDecimal splitRuleValue, UUID addedBy) {
      super("EXPENSE_PARTICIPANT_ADDED", expenseId);
      this.userId = userId;
      this.splitRuleType = splitRuleType;
      this.splitRuleValue = splitRuleValue;
      this.addedBy = addedBy;
      setCausedBy(addedBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ExpenseParticipantRemoved extends ExpenseEvent {
    private UUID userId;
    private UUID removedBy;

    public ExpenseParticipantRemoved(UUID expenseId, UUID userId, UUID removedBy) {
      super("EXPENSE_PARTICIPANT_REMOVED", expenseId);
      this.userId = userId;
      this.removedBy = removedBy;
      setCausedBy(removedBy);
    }
  }

  // Helper classes
  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class ParticipantInfo {
    private UUID userId;
    private String splitRuleType;
    private BigDecimal splitRuleValue;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class SplitInfo {
    private UUID userId;
    private Long amountCents;
    private String currency;
  }
}
