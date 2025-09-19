package com.expenses.svcfx.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcfx.entity.ConversionHistory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

@Schema(description = "Currency conversion information")
public record ConversionDto(
    @Schema(description = "Conversion ID") UUID id,

    @Schema(description = "From currency code", example = "USD") String fromCurrency,

    @Schema(description = "To currency code", example = "EUR") String toCurrency,

    @Schema(description = "Original amount in cents", example = "10000") Long originalAmountCents,

    @Schema(description = "Converted amount in cents", example = "9200") Long convertedAmountCents,

    @Schema(description = "Exchange rate used", example = "0.92") BigDecimal exchangeRate,

    @Schema(description = "Conversion timestamp") ZonedDateTime conversionDate,

    @Schema(description = "User who performed conversion") UUID userId,

    @Schema(description = "Reference type (EXPENSE, SETTLEMENT, etc.)") String referenceType,

    @Schema(description = "Reference ID") UUID referenceId) {

  // Factory method to create from entity
  public static ConversionDto from(ConversionHistory history) {
    return new ConversionDto(
        history.getId(),
        history.getFromCurrency() != null ? history.getFromCurrency().getCode() : null,
        history.getToCurrency() != null ? history.getToCurrency().getCode() : null,
        history.getOriginalAmountCents(),
        history.getConvertedAmountCents(),
        history.getExchangeRate(),
        history.getConversionDate(),
        history.getUserId(),
        history.getReferenceType(),
        history.getReferenceId());
  }

  // Request DTOs
  @Schema(description = "Request to convert currency")
  @Builder
  public record ConvertCurrencyRequest(
      @Schema(description = "From currency code", example = "USD", required = true) @NotBlank(message = "From currency is required") String fromCurrency,

      @Schema(description = "To currency code", example = "EUR", required = true) @NotBlank(message = "To currency is required") String toCurrency,

      @Schema(description = "Amount to convert in cents", example = "10000", required = true) @NotNull(message = "Amount is required") @PositiveOrZero(message = "Amount must be non-negative") Long amountCents,

      @Schema(description = "Reference type (optional)", example = "EXPENSE") String referenceType,

      @Schema(description = "Reference ID (optional)") UUID referenceId) {
    public ConvertCurrencyRequest {
      // Normalize currency codes to uppercase
      fromCurrency = fromCurrency != null ? fromCurrency.toUpperCase() : null;
      toCurrency = toCurrency != null ? toCurrency.toUpperCase() : null;
    }
  }

  @Schema(description = "Request to get exchange rate")
  @Builder
  public record GetExchangeRateRequest(
      @Schema(description = "Base currency code", example = "USD", required = true) @NotBlank(message = "Base currency is required") String baseCurrency,

      @Schema(description = "Target currency code", example = "EUR", required = true) @NotBlank(message = "Target currency is required") String targetCurrency,

      // Optional date - defaults to today
      @Schema(description = "Rate date (optional, defaults to today)", example = "2025-01-01") java.time.LocalDate rateDate) {
    public GetExchangeRateRequest {
      // Normalize currency codes to uppercase
      baseCurrency = baseCurrency != null ? baseCurrency.toUpperCase() : null;
      targetCurrency = targetCurrency != null ? targetCurrency.toUpperCase() : null;
    }
  }

  // Response DTOs
  @Schema(description = "Currency conversion result")
  @Builder
  public record ConversionResult(
      @Schema(description = "From currency", example = "USD") String fromCurrency,

      @Schema(description = "To currency", example = "EUR") String toCurrency,

      @Schema(description = "Original amount in cents", example = "10000") Long originalAmountCents,

      @Schema(description = "Converted amount in cents", example = "9200") Long convertedAmountCents,

      @Schema(description = "Exchange rate used", example = "0.92") BigDecimal exchangeRate,

      @Schema(description = "Original amount formatted", example = "100.00 USD") String originalAmountFormatted,

      @Schema(description = "Converted amount formatted", example = "92.00 EUR") String convertedAmountFormatted,

      @Schema(description = "Conversion timestamp") ZonedDateTime conversionDate,

      @Schema(description = "Rate date used", example = "2025-01-01") java.time.LocalDate rateDate,

      @Schema(description = "Whether same currency conversion", example = "false") Boolean sameCurrency) {

    // Helper method to format amount
    private static String formatAmount(Long amountCents, String currency) {
      if (amountCents == null)
        return "0.00";
      double amount = amountCents / 100.0;
      return String.format("%.2f %s", amount, currency);
    }

    public static ConversionResult create(String fromCurrency, String toCurrency,
        Long originalAmountCents, Long convertedAmountCents,
        BigDecimal exchangeRate, java.time.LocalDate rateDate) {
      return new ConversionResult(
          fromCurrency,
          toCurrency,
          originalAmountCents,
          convertedAmountCents,
          exchangeRate,
          formatAmount(originalAmountCents, fromCurrency),
          formatAmount(convertedAmountCents, toCurrency),
          ZonedDateTime.now(),
          rateDate,
          fromCurrency.equals(toCurrency));
    }
  }

  @Schema(description = "Exchange rate lookup result")
  @Builder
  public record ExchangeRateLookupResult(
      @Schema(description = "Base currency", example = "USD") String baseCurrency,

      @Schema(description = "Target currency", example = "EUR") String targetCurrency,

      @Schema(description = "Exchange rate", example = "0.92") BigDecimal rate,

      @Schema(description = "Rate date", example = "2025-01-01") java.time.LocalDate rateDate,

      @Schema(description = "Rate source", example = "MANUAL") String source,

      @Schema(description = "Whether rate was found", example = "true") Boolean found,

      @Schema(description = "Error message if rate not found") String errorMessage) {
  }

  @Schema(description = "Conversion statistics")
  @Builder
  public record ConversionStats(
      @Schema(description = "Total conversions count") Long totalConversions,

      @Schema(description = "Most used from currency") String mostUsedFromCurrency,

      @Schema(description = "Most used to currency") String mostUsedToCurrency,

      @Schema(description = "Total amount converted (in cents)") Long totalAmountConverted,

      @Schema(description = "Average conversion amount (in cents)") Long averageConversionAmount) {
  }
}
