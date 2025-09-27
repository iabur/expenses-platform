package com.expenses.svcsplitengine.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;

/**
 * DTO for debt relationships between users
 */
@Builder
public record DebtSummary(
    UUID debtorId,
    UUID creditorId,
    BigDecimal amount,
    String currency,
    String description) {

  /**
   * Get amount in cents
   */
  public Long getAmountCents() {
    return amount != null ? amount.multiply(BigDecimal.valueOf(100)).longValue() : 0L;
  }

  /**
   * Get amount as decimal
   */
  public BigDecimal getAmountDecimal() {
    return amount != null ? amount : BigDecimal.ZERO;
  }
}
