package com.expenses.svcfx.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcfx.entity.ExchangeRate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

@Schema(description = "Exchange rate information")
public record ExchangeRateDto(
    @Schema(description = "Exchange rate ID") UUID id,

    @Schema(description = "Base currency code", example = "USD") String baseCurrency,

    @Schema(description = "Target currency code", example = "EUR") String targetCurrency,

    @Schema(description = "Exchange rate", example = "0.92") BigDecimal rate,

    @Schema(description = "Rate date", example = "2025-01-01") LocalDate rateDate,

    @Schema(description = "Rate source", example = "MANUAL") String source,

    @Schema(description = "Whether rate is active", example = "true") Boolean isActive,

    @Schema(description = "Creation timestamp") ZonedDateTime createdAt,

    @Schema(description = "Last update timestamp") ZonedDateTime updatedAt) {

  // Factory method to create from entity
  public static ExchangeRateDto from(ExchangeRate exchangeRate) {
    return new ExchangeRateDto(
        exchangeRate.getId(),
        exchangeRate.getBaseCurrency() != null ? exchangeRate.getBaseCurrency().getCode() : null,
        exchangeRate.getTargetCurrency() != null ? exchangeRate.getTargetCurrency().getCode() : null,
        exchangeRate.getRate(),
        exchangeRate.getRateDate(),
        exchangeRate.getSource(),
        exchangeRate.getIsActive(),
        exchangeRate.getCreatedAt(),
        exchangeRate.getUpdatedAt());
  }

  // Request DTOs
  @Schema(description = "Request to create or update an exchange rate")
  @Builder
  public record CreateExchangeRateRequest(
      @Schema(description = "Base currency code", example = "USD", required = true) @NotBlank(message = "Base currency is required") String baseCurrency,

      @Schema(description = "Target currency code", example = "EUR", required = true) @NotBlank(message = "Target currency is required") String targetCurrency,

      @Schema(description = "Exchange rate", example = "0.92", required = true) @NotNull(message = "Exchange rate is required") @Positive(message = "Exchange rate must be positive") BigDecimal rate,

      @Schema(description = "Rate date (defaults to today)", example = "2025-01-01") LocalDate rateDate,

      @Schema(description = "Rate source", example = "MANUAL") String source) {
    public CreateExchangeRateRequest {
      if (rateDate == null) {
        rateDate = LocalDate.now();
      }
      if (source == null || source.isBlank()) {
        source = "MANUAL";
      }
      // Normalize currency codes to uppercase
      baseCurrency = baseCurrency != null ? baseCurrency.toUpperCase() : null;
      targetCurrency = targetCurrency != null ? targetCurrency.toUpperCase() : null;
    }
  }

  @Schema(description = "Request to update an exchange rate")
  @Builder
  public record UpdateExchangeRateRequest(
      @Schema(description = "Exchange rate", example = "0.93") @Positive(message = "Exchange rate must be positive") BigDecimal rate,

      @Schema(description = "Rate date", example = "2025-01-01") LocalDate rateDate,

      @Schema(description = "Rate source", example = "API") String source,

      @Schema(description = "Whether rate is active", example = "true") Boolean isActive) {
    public void updateEntity(ExchangeRate exchangeRate) {
      if (rate != null)
        exchangeRate.setRate(rate);
      if (rateDate != null)
        exchangeRate.setRateDate(rateDate);
      if (source != null)
        exchangeRate.setSource(source);
      if (isActive != null)
        exchangeRate.setIsActive(isActive);
    }
  }

  // Response DTOs
  @Schema(description = "Exchange rate information for currency conversion")
  @Builder
  public record ExchangeRateInfo(
      @Schema(description = "Base currency", example = "USD") String baseCurrency,

      @Schema(description = "Target currency", example = "EUR") String targetCurrency,

      @Schema(description = "Exchange rate", example = "0.92") BigDecimal rate,

      @Schema(description = "Rate date", example = "2025-01-01") LocalDate rateDate,

      @Schema(description = "Rate source", example = "MANUAL") String source,

      @Schema(description = "Whether this is the latest rate", example = "true") Boolean isLatest) {
    public static ExchangeRateInfo from(ExchangeRate exchangeRate, boolean isLatest) {
      return new ExchangeRateInfo(
          exchangeRate.getBaseCurrency() != null ? exchangeRate.getBaseCurrency().getCode() : null,
          exchangeRate.getTargetCurrency() != null ? exchangeRate.getTargetCurrency().getCode() : null,
          exchangeRate.getRate(),
          exchangeRate.getRateDate(),
          exchangeRate.getSource(),
          isLatest);
    }
  }

  @Schema(description = "Historical exchange rates")
  @Builder
  public record ExchangeRateHistory(
      @Schema(description = "Base currency", example = "USD") String baseCurrency,

      @Schema(description = "Target currency", example = "EUR") String targetCurrency,

      @Schema(description = "Historical rates") java.util.List<ExchangeRateInfo> rates) {
  }
}
