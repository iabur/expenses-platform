package com.expenses.svcsettle.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "settlement_payments", uniqueConstraints = @UniqueConstraint(columnNames = { "proposal_id", "payer_id",
    "payee_id" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "proposal", "confirmations" })
public class SettlementPayment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "proposal_id", nullable = false)
  @NotNull(message = "Proposal is required")
  private SettlementProposal proposal;

  @Column(name = "payer_id", nullable = false)
  @NotNull(message = "Payer ID is required")
  private UUID payerId;

  @Column(name = "payee_id", nullable = false)
  @NotNull(message = "Payee ID is required")
  private UUID payeeId;

  @Column(length = 3, nullable = false)
  @NotBlank(message = "Currency is required")
  @Size(min = 3, max = 3, message = "Currency must be 3 characters")
  private String currency;

  @Column(name = "amount_cents", nullable = false)
  @Positive(message = "Amount must be positive")
  private Long amountCents;

  @Column(columnDefinition = "TEXT")
  @Size(max = 1000, message = "Description cannot exceed 1000 characters")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private PaymentStatus status = PaymentStatus.PENDING;

  @Column(name = "payment_method")
  @Size(max = 50, message = "Payment method cannot exceed 50 characters")
  private String paymentMethod;

  @Column(name = "payment_reference")
  @Size(max = 100, message = "Payment reference cannot exceed 100 characters")
  private String paymentReference;

  @Column(name = "due_date")
  private ZonedDateTime dueDate;

  @Column(name = "completed_at")
  private ZonedDateTime completedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @Builder.Default
  private List<PaymentConfirmation> confirmations = new ArrayList<>();

  // Enums
  public enum PaymentStatus {
    PENDING, // Waiting for payment
    CONFIRMED, // Payer confirmed payment made
    COMPLETED, // Payee confirmed payment received
    FAILED, // Payment failed
    CANCELLED // Payment cancelled
  }

  // Helper methods
  public BigDecimal getAmountDecimal() {
    return BigDecimal.valueOf(amountCents, 2);
  }

  public void setAmountDecimal(BigDecimal amount) {
    this.amountCents = amount.movePointRight(2).longValue();
  }

  public void addConfirmation(PaymentConfirmation confirmation) {
    confirmations.add(confirmation);
    confirmation.setPayment(this);
  }

  public void removeConfirmation(PaymentConfirmation confirmation) {
    confirmations.remove(confirmation);
    confirmation.setPayment(null);
  }

  public boolean isOverdue() {
    return dueDate != null &&
        ZonedDateTime.now().isAfter(dueDate) &&
        status != PaymentStatus.COMPLETED;
  }

  public boolean canBeConfirmed() {
    return status == PaymentStatus.PENDING;
  }

  public boolean canBeCompleted() {
    return status == PaymentStatus.CONFIRMED;
  }

  public boolean hasPayerConfirmation() {
    return confirmations.stream()
        .anyMatch(c -> c.getConfirmationType() == PaymentConfirmation.ConfirmationType.PAYER_CONFIRMED);
  }

  public boolean hasPayeeConfirmation() {
    return confirmations.stream()
        .anyMatch(c -> c.getConfirmationType() == PaymentConfirmation.ConfirmationType.PAYEE_CONFIRMED);
  }

  public boolean isDisputed() {
    return confirmations.stream()
        .anyMatch(c -> c.getConfirmationType() == PaymentConfirmation.ConfirmationType.DISPUTED);
  }

  public void confirmByPayer(UUID payerId, String notes, String attachmentUrl) {
    if (!this.payerId.equals(payerId)) {
      throw new IllegalArgumentException("Only the payer can confirm payment");
    }

    if (canBeConfirmed()) {
      this.status = PaymentStatus.CONFIRMED;

      PaymentConfirmation confirmation = PaymentConfirmation.builder()
          .payment(this)
          .confirmerId(payerId)
          .confirmationType(PaymentConfirmation.ConfirmationType.PAYER_CONFIRMED)
          .notes(notes)
          .attachmentUrl(attachmentUrl)
          .build();

      addConfirmation(confirmation);
    } else {
      throw new IllegalStateException("Payment cannot be confirmed in current state");
    }
  }

  public void confirmByPayee(UUID payeeId, String notes) {
    if (!this.payeeId.equals(payeeId)) {
      throw new IllegalArgumentException("Only the payee can confirm receipt");
    }

    if (canBeCompleted()) {
      this.status = PaymentStatus.COMPLETED;
      this.completedAt = ZonedDateTime.now();

      PaymentConfirmation confirmation = PaymentConfirmation.builder()
          .payment(this)
          .confirmerId(payeeId)
          .confirmationType(PaymentConfirmation.ConfirmationType.PAYEE_CONFIRMED)
          .notes(notes)
          .build();

      addConfirmation(confirmation);
    } else {
      throw new IllegalStateException("Payment cannot be completed in current state");
    }
  }

  public void dispute(UUID userId, String reason) {
    if (!userId.equals(payerId) && !userId.equals(payeeId)) {
      throw new IllegalArgumentException("Only payer or payee can dispute payment");
    }

    PaymentConfirmation dispute = PaymentConfirmation.builder()
        .payment(this)
        .confirmerId(userId)
        .confirmationType(PaymentConfirmation.ConfirmationType.DISPUTED)
        .notes(reason)
        .build();

    addConfirmation(dispute);
  }

  public void cancel() {
    if (status == PaymentStatus.PENDING || status == PaymentStatus.CONFIRMED) {
      this.status = PaymentStatus.CANCELLED;
    } else {
      throw new IllegalStateException("Cannot cancel payment in current state");
    }
  }

  public void markAsFailed(String reason) {
    if (status != PaymentStatus.COMPLETED) {
      this.status = PaymentStatus.FAILED;

      PaymentConfirmation failure = PaymentConfirmation.builder()
          .payment(this)
          .confirmerId(payerId) // System or payer reporting failure
          .confirmationType(PaymentConfirmation.ConfirmationType.RESOLVED)
          .notes("Payment failed: " + reason)
          .build();

      addConfirmation(failure);
    }
  }
}
