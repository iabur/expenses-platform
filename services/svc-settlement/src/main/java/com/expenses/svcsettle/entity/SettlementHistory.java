package com.expenses.svcsettle.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "settlement_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "group_id", nullable = false)
  private UUID groupId;

  @Column(name = "proposal_id")
  private UUID proposalId;

  @Column(name = "payment_id")
  private UUID paymentId;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "action_type", nullable = false)
  private ActionType actionType;

  @Enumerated(EnumType.STRING)
  @Column(name = "entity_type", nullable = false)
  private EntityType entityType;

  @Column(name = "description", length = 1000)
  private String description;

  @Column(name = "amount_cents")
  private Long amountCents;

  @Column(name = "currency", length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private String status;

  @Column(name = "metadata", length = 2000)
  private String metadata;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  public enum ActionType {
    CREATED,
    UPDATED,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    COMPLETED,
    CONFIRMED,
    DISPUTED,
    EXPIRED
  }

  public enum EntityType {
    PROPOSAL,
    PAYMENT,
    CONFIRMATION,
    DISPUTE
  }

  public BigDecimal getAmountDecimal() {
    return amountCents != null ? BigDecimal.valueOf(amountCents).divide(BigDecimal.valueOf(100)) : null;
  }

  public void setAmountDecimal(BigDecimal amount) {
    this.amountCents = amount != null ? amount.multiply(BigDecimal.valueOf(100)).longValue() : null;
  }
}
