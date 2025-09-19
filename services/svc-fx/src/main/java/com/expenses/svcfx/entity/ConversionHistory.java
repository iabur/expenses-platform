package com.expenses.svcfx.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "conversion_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionHistory {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @NotNull(message = "From currency is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "from_currency", referencedColumnName = "code", nullable = false)
  private Currency fromCurrency;

  @NotNull(message = "To currency is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "to_currency", referencedColumnName = "code", nullable = false)
  private Currency toCurrency;

  @NotNull(message = "Original amount is required")
  @PositiveOrZero(message = "Original amount must be non-negative")
  @Column(name = "original_amount_cents", nullable = false)
  private Long originalAmountCents;

  @NotNull(message = "Converted amount is required")
  @PositiveOrZero(message = "Converted amount must be non-negative")
  @Column(name = "converted_amount_cents", nullable = false)
  private Long convertedAmountCents;

  @NotNull(message = "Exchange rate is required")
  @Column(name = "exchange_rate", precision = 20, scale = 8, nullable = false)
  private BigDecimal exchangeRate;

  @CreationTimestamp
  @Column(name = "conversion_date", nullable = false, updatable = false)
  private ZonedDateTime conversionDate;

  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "reference_type", length = 50)
  private String referenceType;

  @Column(name = "reference_id")
  private UUID referenceId;

  // Business methods
  public boolean isSameCurrencyConversion() {
    return fromCurrency != null && toCurrency != null
        && fromCurrency.getCode().equals(toCurrency.getCode());
  }

  public double getEffectiveRate() {
    if (originalAmountCents == null || originalAmountCents == 0) {
      return exchangeRate != null ? exchangeRate.doubleValue() : 0.0;
    }

    return convertedAmountCents.doubleValue() / originalAmountCents.doubleValue();
  }

  public String getConversionSummary() {
    return String.format("Converted %s %s to %s %s at rate %s",
        formatAmount(originalAmountCents),
        fromCurrency != null ? fromCurrency.getCode() : "?",
        formatAmount(convertedAmountCents),
        toCurrency != null ? toCurrency.getCode() : "?",
        exchangeRate);
  }

  private String formatAmount(Long amountCents) {
    if (amountCents == null)
      return "0";
    return String.format("%.2f", amountCents / 100.0);
  }

  // Builder helper methods
  public static ConversionHistory.ConversionHistoryBuilder forExpense(UUID expenseId, UUID userId) {
    return ConversionHistory.builder()
        .referenceType("EXPENSE")
        .referenceId(expenseId)
        .userId(userId);
  }

  public static ConversionHistory.ConversionHistoryBuilder forSettlement(UUID settlementId, UUID userId) {
    return ConversionHistory.builder()
        .referenceType("SETTLEMENT")
        .referenceId(settlementId)
        .userId(userId);
  }

  @Override
  public String toString() {
    return String.format("ConversionHistory{%s->%s: %s@%s on %s}",
        fromCurrency != null ? fromCurrency.getCode() : "null",
        toCurrency != null ? toCurrency.getCode() : "null",
        formatAmount(originalAmountCents),
        exchangeRate,
        conversionDate);
  }
}
