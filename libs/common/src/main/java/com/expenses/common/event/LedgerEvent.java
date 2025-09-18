package com.expenses.common.event;

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
    @JsonSubTypes.Type(value = LedgerEvent.JournalEntryCreated.class, name = "JOURNAL_ENTRY_CREATED"),
    @JsonSubTypes.Type(value = LedgerEvent.AccountBalanceUpdated.class, name = "ACCOUNT_BALANCE_UPDATED"),
    @JsonSubTypes.Type(value = LedgerEvent.SettlementProposalGenerated.class, name = "SETTLEMENT_PROPOSAL_GENERATED")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public abstract class LedgerEvent extends DomainEvent {

  public static final String TOPIC = "ledger-events";

  protected LedgerEvent(String eventName, UUID aggregateId) {
    super("LEDGER", eventName, aggregateId, "Ledger");
  }

  @Override
  public String getTopicName() {
    return TOPIC;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class JournalEntryCreated extends LedgerEvent {
    private UUID journalEntryId;
    private String referenceType; // EXPENSE, PAYMENT, SETTLEMENT
    private UUID referenceId;
    private UUID groupId;
    private String description;
    private LocalDate valueDate;
    private List<PostingInfo> postings;
    private UUID createdBy;

    public JournalEntryCreated(UUID journalEntryId, String referenceType, UUID referenceId,
        UUID groupId, String description, LocalDate valueDate,
        List<PostingInfo> postings, UUID createdBy) {
      super("JOURNAL_ENTRY_CREATED", journalEntryId);
      this.journalEntryId = journalEntryId;
      this.referenceType = referenceType;
      this.referenceId = referenceId;
      this.groupId = groupId;
      this.description = description;
      this.valueDate = valueDate;
      this.postings = postings;
      this.createdBy = createdBy;
      setCausedBy(createdBy);
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class AccountBalanceUpdated extends LedgerEvent {
    private UUID accountId;
    private String accountCode;
    private String ownerType; // USER, GROUP
    private UUID ownerId;
    private String currency;
    private Long previousBalanceCents;
    private Long newBalanceCents;
    private Long changeAmountCents;
    private UUID journalEntryId;

    public AccountBalanceUpdated(UUID accountId, String accountCode, String ownerType,
        UUID ownerId, String currency, Long previousBalanceCents,
        Long newBalanceCents, Long changeAmountCents, UUID journalEntryId) {
      super("ACCOUNT_BALANCE_UPDATED", accountId);
      this.accountId = accountId;
      this.accountCode = accountCode;
      this.ownerType = ownerType;
      this.ownerId = ownerId;
      this.currency = currency;
      this.previousBalanceCents = previousBalanceCents;
      this.newBalanceCents = newBalanceCents;
      this.changeAmountCents = changeAmountCents;
      this.journalEntryId = journalEntryId;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class SettlementProposalGenerated extends LedgerEvent {
    private UUID settlementProposalId;
    private UUID groupId;
    private List<TransferInfo> transfers;
    private Integer totalTransfers;
    private UUID generatedBy;

    public SettlementProposalGenerated(UUID settlementProposalId, UUID groupId,
        List<TransferInfo> transfers, Integer totalTransfers,
        UUID generatedBy) {
      super("SETTLEMENT_PROPOSAL_GENERATED", settlementProposalId);
      this.settlementProposalId = settlementProposalId;
      this.groupId = groupId;
      this.transfers = transfers;
      this.totalTransfers = totalTransfers;
      this.generatedBy = generatedBy;
      setCausedBy(generatedBy);
    }
  }

  // Helper classes
  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class PostingInfo {
    private UUID postingId;
    private UUID debitAccountId;
    private UUID creditAccountId;
    private Long amountCents;
    private String currency;
    private String narrative;
  }

  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class TransferInfo {
    private UUID fromUserId;
    private UUID toUserId;
    private Long amountCents;
    private String currency;
    private String suggestedMethod;
  }
}
