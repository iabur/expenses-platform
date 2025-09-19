package com.expenses.svcfx.dto;

import java.time.ZonedDateTime;

import com.expenses.svcfx.entity.Currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "Currency information")
public record CurrencyDto(
    @Schema(description = "Currency code (ISO 4217)", example = "USD") @NotBlank(message = "Currency code is required") @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters") String code,

    @Schema(description = "Currency name", example = "US Dollar") @NotBlank(message = "Currency name is required") String name,

    @Schema(description = "Currency symbol", example = "$") String symbol,

    @Schema(description = "Number of decimal places", example = "2") Integer decimalPlaces,

    @Schema(description = "Whether currency is active", example = "true") Boolean isActive,

    @Schema(description = "Creation timestamp") ZonedDateTime createdAt,

    @Schema(description = "Last update timestamp") ZonedDateTime updatedAt) {

  // Factory method to create from entity
  public static CurrencyDto from(Currency currency) {
    return new CurrencyDto(
        currency.getCode(),
        currency.getName(),
        currency.getSymbol(),
        currency.getDecimalPlaces(),
        currency.getIsActive(),
        currency.getCreatedAt(),
        currency.getUpdatedAt());
  }

  // Convert to entity
  public Currency toEntity() {
    return Currency.builder()
        .code(code)
        .name(name)
        .symbol(symbol)
        .decimalPlaces(decimalPlaces)
        .isActive(isActive != null ? isActive : true)
        .build();
  }

  // Create/Update requests
  @Schema(description = "Request to create a new currency")
  @Builder
  public record CreateCurrencyRequest(
      @Schema(description = "Currency code (ISO 4217)", example = "USD", required = true) @NotBlank(message = "Currency code is required") @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters") String code,

      @Schema(description = "Currency name", example = "US Dollar", required = true) @NotBlank(message = "Currency name is required") String name,

      @Schema(description = "Currency symbol", example = "$") String symbol,

      @Schema(description = "Number of decimal places", example = "2") Integer decimalPlaces) {
    public Currency toEntity() {
      return Currency.builder()
          .code(code.toUpperCase())
          .name(name)
          .symbol(symbol)
          .decimalPlaces(decimalPlaces != null ? decimalPlaces : 2)
          .isActive(true)
          .build();
    }
  }

  @Schema(description = "Request to update an existing currency")
  @Builder
  public record UpdateCurrencyRequest(
      @Schema(description = "Currency name", example = "US Dollar") String name,

      @Schema(description = "Currency symbol", example = "$") String symbol,

      @Schema(description = "Number of decimal places", example = "2") Integer decimalPlaces,

      @Schema(description = "Whether currency is active", example = "true") Boolean isActive) {
    public void updateEntity(Currency currency) {
      if (name != null)
        currency.setName(name);
      if (symbol != null)
        currency.setSymbol(symbol);
      if (decimalPlaces != null)
        currency.setDecimalPlaces(decimalPlaces);
      if (isActive != null)
        currency.setIsActive(isActive);
    }
  }

  // Response DTOs
  @Schema(description = "Simplified currency information for dropdowns")
  @Builder
  public record CurrencyOption(
      @Schema(description = "Currency code", example = "USD") String code,

      @Schema(description = "Currency name", example = "US Dollar") String name,

      @Schema(description = "Currency symbol", example = "$") String symbol) {
    public static CurrencyOption from(Currency currency) {
      return new CurrencyOption(
          currency.getCode(),
          currency.getName(),
          currency.getSymbol());
    }
  }
}
