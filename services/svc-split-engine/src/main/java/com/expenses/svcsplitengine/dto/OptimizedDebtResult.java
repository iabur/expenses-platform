package com.expenses.svcsplitengine.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Builder;

/**
 * DTO for optimized debt settlement result
 */
@Builder
public record OptimizedDebtResult(
    UUID groupId,
    String currency,
    BigDecimal totalDebtAmount,
    BigDecimal totalCreditAmount,
    Integer originalTransactionCount,
    Integer optimizedTransactionCount,
    BigDecimal savingsPercentage,
    List<OptimizedTransaction> transactions) {

  /**
   * Individual optimized transaction
   */
  @Builder
  public static record OptimizedTransaction(
      UUID payerId,
      UUID payeeId,
      BigDecimal amount,
      String currency,
      String description) {

    /**
     * Get amount in cents
     */
    public Long getAmountCents() {
      return amount != null ? amount.multiply(BigDecimal.valueOf(100)).longValue() : 0L;
    }
  }

  /**
   * Get total debt amount in cents
   */
  public Long getTotalDebtAmountCents() {
    return totalDebtAmount != null ? totalDebtAmount.multiply(BigDecimal.valueOf(100)).longValue() : 0L;
  }

  /**
   * Get total credit amount in cents
   */
  public Long getTotalCreditAmountCents() {
    return totalCreditAmount != null ? totalCreditAmount.multiply(BigDecimal.valueOf(100)).longValue() : 0L;
  }

  /**
   * Get optimization summary
   */
  public String getSummary() {
    return String.format("Optimized %d transactions down to %d (%.1f%% reduction)",
        originalTransactionCount, optimizedTransactionCount, savingsPercentage);
  }
}
