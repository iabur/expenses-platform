package com.expenses.svcfx.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "exchange_rates", uniqueConstraints = @UniqueConstraint(columnNames = { "base_currency",
    "target_currency", "rate_date" }))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRate {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @NotNull(message = "Base currency is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "base_currency", referencedColumnName = "code", nullable = false)
  private Currency baseCurrency;

  @NotNull(message = "Target currency is required")
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "target_currency", referencedColumnName = "code", nullable = false)
  private Currency targetCurrency;

  @NotNull(message = "Exchange rate is required")
  @Positive(message = "Exchange rate must be positive")
  @Column(name = "rate", precision = 20, scale = 8, nullable = false)
  private BigDecimal rate;

  @NotNull(message = "Rate date is required")
  @Column(name = "rate_date", nullable = false)
  private LocalDate rateDate;

  @Builder.Default
  @Column(name = "source", length = 50)
  private String source = "MANUAL";

  @Builder.Default
  @NotNull
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Business methods
  public boolean isActive() {
    return Boolean.TRUE.equals(isActive);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public boolean isSameCurrency() {
    return baseCurrency != null && targetCurrency != null
        && baseCurrency.getCode().equals(targetCurrency.getCode());
  }

  public boolean isCurrentRate() {
    return rateDate != null && rateDate.equals(LocalDate.now());
  }

  // Convert amount using this exchange rate
  public long convertAmount(long amountCents) {
    if (rate == null) {
      throw new IllegalStateException("Exchange rate is null");
    }

    BigDecimal amount = BigDecimal.valueOf(amountCents);
    BigDecimal convertedAmount = amount.multiply(rate);

    return convertedAmount.longValue();
  }

  @Override
  public String toString() {
    return String.format("ExchangeRate{%s->%s: %s on %s}",
        baseCurrency != null ? baseCurrency.getCode() : "null",
        targetCurrency != null ? targetCurrency.getCode() : "null",
        rate, rateDate);
  }
}
