package com.expenses.svcsettle.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "payment_confirmations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = "payment")
public class PaymentConfirmation {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "payment_id", nullable = false)
  @NotNull(message = "Payment is required")
  private SettlementPayment payment;

  @Column(name = "confirmer_id", nullable = false)
  @NotNull(message = "Confirmer ID is required")
  private UUID confirmerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "confirmation_type", nullable = false)
  @NotNull(message = "Confirmation type is required")
  private ConfirmationType confirmationType;

  @Column(columnDefinition = "TEXT")
  @Size(max = 2000, message = "Notes cannot exceed 2000 characters")
  private String notes;

  @Column(name = "attachment_url")
  @Size(max = 500, message = "Attachment URL cannot exceed 500 characters")
  private String attachmentUrl;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  // Enums
  public enum ConfirmationType {
    PAYER_CONFIRMED, // Payer confirms payment was sent
    PAYEE_CONFIRMED, // Payee confirms payment was received
    DISPUTED, // Payment is disputed
    RESOLVED // Dispute resolved or system confirmation
  }

  // Helper methods
  public boolean isConfirmation() {
    return confirmationType == ConfirmationType.PAYER_CONFIRMED ||
        confirmationType == ConfirmationType.PAYEE_CONFIRMED;
  }

  public boolean isDispute() {
    return confirmationType == ConfirmationType.DISPUTED;
  }

  public boolean isResolution() {
    return confirmationType == ConfirmationType.RESOLVED;
  }
}
