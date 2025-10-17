package com.expenses.common.event;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all settlement-related domain events.
 * These events are published by the Settlement Service and consumed by other
 * services.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventName")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SettlementEvent.ProposalCreated.class, name = "PROPOSAL_CREATED"),
    @JsonSubTypes.Type(value = SettlementEvent.ProposalAccepted.class, name = "PROPOSAL_ACCEPTED"),
    @JsonSubTypes.Type(value = SettlementEvent.ProposalRejected.class, name = "PROPOSAL_REJECTED"),
    @JsonSubTypes.Type(value = SettlementEvent.ProposalCancelled.class, name = "PROPOSAL_CANCELLED"),
    @JsonSubTypes.Type(value = SettlementEvent.ProposalCompleted.class, name = "PROPOSAL_COMPLETED"),
    @JsonSubTypes.Type(value = SettlementEvent.PaymentConfirmed.class, name = "PAYMENT_CONFIRMED"),
    @JsonSubTypes.Type(value = SettlementEvent.PaymentCompleted.class, name = "PAYMENT_COMPLETED"),
    @JsonSubTypes.Type(value = SettlementEvent.PaymentDisputed.class, name = "PAYMENT_DISPUTED")
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public abstract class SettlementEvent extends DomainEvent {

  public static final String TOPIC = "settlement-events";

  protected SettlementEvent(String eventName, UUID aggregateId) {
    super("SETTLEMENT", eventName, aggregateId, "Settlement");
  }

  @Override
  public String getTopicName() {
    return TOPIC;
  }

  /**
   * Event published when a settlement proposal is created
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ProposalCreated extends SettlementEvent {
    private UUID proposalId;
    private UUID groupId;
    private UUID proposerId;
    private String title;
    private String description;
    private String currency;
    private Long totalAmountCents;
    private String proposalType;
    private List<PaymentInfo> payments;
    private ZonedDateTime expiresAt;

    public ProposalCreated(UUID proposalId, UUID groupId, UUID proposerId,
        String title, String description, String currency,
        Long totalAmountCents, String proposalType,
        List<PaymentInfo> payments, ZonedDateTime expiresAt) {
      super("PROPOSAL_CREATED", proposalId);
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.proposerId = proposerId;
      this.title = title;
      this.description = description;
      this.currency = currency;
      this.totalAmountCents = totalAmountCents;
      this.proposalType = proposalType;
      this.payments = payments;
      this.expiresAt = expiresAt;
      setCausedBy(proposerId);
    }
  }

  /**
   * Event published when a settlement proposal is accepted
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ProposalAccepted extends SettlementEvent {
    private UUID proposalId;
    private UUID groupId;
    private UUID acceptedBy;
    private String previousStatus;
    private ZonedDateTime acceptedAt;

    public ProposalAccepted(UUID proposalId, UUID groupId, UUID acceptedBy,
        String previousStatus, ZonedDateTime acceptedAt) {
      super("PROPOSAL_ACCEPTED", proposalId);
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.acceptedBy = acceptedBy;
      this.previousStatus = previousStatus;
      this.acceptedAt = acceptedAt;
      setCausedBy(acceptedBy);
    }
  }

  /**
   * Event published when a settlement proposal is rejected
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ProposalRejected extends SettlementEvent {
    private UUID proposalId;
    private UUID groupId;
    private UUID rejectedBy;
    private String reason;
    private ZonedDateTime rejectedAt;

    public ProposalRejected(UUID proposalId, UUID groupId, UUID rejectedBy,
        String reason, ZonedDateTime rejectedAt) {
      super("PROPOSAL_REJECTED", proposalId);
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.rejectedBy = rejectedBy;
      this.reason = reason;
      this.rejectedAt = rejectedAt;
      setCausedBy(rejectedBy);
    }
  }

  /**
   * Event published when a settlement proposal is cancelled
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ProposalCancelled extends SettlementEvent {
    private UUID proposalId;
    private UUID groupId;
    private UUID cancelledBy;
    private String reason;
    private ZonedDateTime cancelledAt;

    public ProposalCancelled(UUID proposalId, UUID groupId, UUID cancelledBy,
        String reason, ZonedDateTime cancelledAt) {
      super("PROPOSAL_CANCELLED", proposalId);
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.cancelledBy = cancelledBy;
      this.reason = reason;
      this.cancelledAt = cancelledAt;
      setCausedBy(cancelledBy);
    }
  }

  /**
   * Event published when all payments in a proposal are completed
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class ProposalCompleted extends SettlementEvent {
    private UUID proposalId;
    private UUID groupId;
    private Long totalAmountCents;
    private String currency;
    private Integer paymentCount;
    private ZonedDateTime completedAt;

    public ProposalCompleted(UUID proposalId, UUID groupId, Long totalAmountCents,
        String currency, Integer paymentCount, ZonedDateTime completedAt) {
      super("PROPOSAL_COMPLETED", proposalId);
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.totalAmountCents = totalAmountCents;
      this.currency = currency;
      this.paymentCount = paymentCount;
      this.completedAt = completedAt;
    }
  }

  /**
   * Event published when a payment is confirmed by payer or payee
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class PaymentConfirmed extends SettlementEvent {
    private UUID paymentId;
    private UUID proposalId;
    private UUID groupId;
    private UUID payerId;
    private UUID payeeId;
    private UUID confirmedBy;
    private String confirmationType; // PAYER_CONFIRMED, PAYEE_CONFIRMED
    private Long amountCents;
    private String currency;
    private String paymentMethod;
    private String notes;
    private ZonedDateTime confirmedAt;

    public PaymentConfirmed(UUID paymentId, UUID proposalId, UUID groupId,
        UUID payerId, UUID payeeId, UUID confirmedBy,
        String confirmationType, Long amountCents, String currency,
        String paymentMethod, String notes, ZonedDateTime confirmedAt) {
      super("PAYMENT_CONFIRMED", paymentId);
      this.paymentId = paymentId;
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.payerId = payerId;
      this.payeeId = payeeId;
      this.confirmedBy = confirmedBy;
      this.confirmationType = confirmationType;
      this.amountCents = amountCents;
      this.currency = currency;
      this.paymentMethod = paymentMethod;
      this.notes = notes;
      this.confirmedAt = confirmedAt;
      setCausedBy(confirmedBy);
    }
  }

  /**
   * Event published when a payment is completed (both payer and payee confirmed)
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class PaymentCompleted extends SettlementEvent {
    private UUID paymentId;
    private UUID proposalId;
    private UUID groupId;
    private UUID payerId;
    private UUID payeeId;
    private Long amountCents;
    private String currency;
    private String paymentMethod;
    private String paymentReference;
    private ZonedDateTime completedAt;

    public PaymentCompleted(UUID paymentId, UUID proposalId, UUID groupId,
        UUID payerId, UUID payeeId, Long amountCents,
        String currency, String paymentMethod, String paymentReference,
        ZonedDateTime completedAt) {
      super("PAYMENT_COMPLETED", paymentId);
      this.paymentId = paymentId;
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.payerId = payerId;
      this.payeeId = payeeId;
      this.amountCents = amountCents;
      this.currency = currency;
      this.paymentMethod = paymentMethod;
      this.paymentReference = paymentReference;
      this.completedAt = completedAt;
    }
  }

  /**
   * Event published when a payment is disputed
   */
  @Data
  @EqualsAndHashCode(callSuper = true)
  @SuperBuilder
  @NoArgsConstructor
  public static class PaymentDisputed extends SettlementEvent {
    private UUID paymentId;
    private UUID proposalId;
    private UUID groupId;
    private UUID payerId;
    private UUID payeeId;
    private UUID disputedBy;
    private String reason;
    private Long amountCents;
    private String currency;
    private ZonedDateTime disputedAt;

    public PaymentDisputed(UUID paymentId, UUID proposalId, UUID groupId,
        UUID payerId, UUID payeeId, UUID disputedBy,
        String reason, Long amountCents, String currency,
        ZonedDateTime disputedAt) {
      super("PAYMENT_DISPUTED", paymentId);
      this.paymentId = paymentId;
      this.proposalId = proposalId;
      this.groupId = groupId;
      this.payerId = payerId;
      this.payeeId = payeeId;
      this.disputedBy = disputedBy;
      this.reason = reason;
      this.amountCents = amountCents;
      this.currency = currency;
      this.disputedAt = disputedAt;
      setCausedBy(disputedBy);
    }
  }

  // Helper classes
  @Data
  @SuperBuilder
  @NoArgsConstructor
  public static class PaymentInfo {
    private UUID paymentId;
    private UUID payerId;
    private UUID payeeId;
    private Long amountCents;
    private String currency;
    private String description;
    private String paymentMethod;
  }
}

