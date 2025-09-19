package com.expenses.svcfx.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcfx.dto.ConversionDto;
import com.expenses.svcfx.dto.CurrencyDto;
import com.expenses.svcfx.dto.ExchangeRateDto;
import com.expenses.svcfx.service.ConversionService;
import com.expenses.svcfx.service.CurrencyService;
import com.expenses.svcfx.service.ExchangeRateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/fx")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "FX Service", description = "Currency exchange rates and conversion")
public class FxController {

  private final CurrencyService currencyService;
  private final ExchangeRateService exchangeRateService;
  private final ConversionService conversionService;

  // Currency endpoints
  @GetMapping("/currencies")
  @Operation(summary = "Get all active currencies", description = "Retrieve all active currencies")
  @ApiResponse(responseCode = "200", description = "Currencies retrieved successfully")
  public ResponseEntity<List<CurrencyDto>> getAllCurrencies() {
    List<CurrencyDto> currencies = currencyService.getAllActiveCurrencies();
    return ResponseEntity.ok(currencies);
  }

  @GetMapping("/currencies/common")
  @Operation(summary = "Get common currencies", description = "Get commonly used currencies for UI dropdowns")
  @ApiResponse(responseCode = "200", description = "Common currencies retrieved successfully")
  public ResponseEntity<List<CurrencyDto.CurrencyOption>> getCommonCurrencies() {
    List<CurrencyDto.CurrencyOption> currencies = currencyService.getCommonCurrencies();
    return ResponseEntity.ok(currencies);
  }

  @GetMapping("/currencies/{code}")
  @Operation(summary = "Get currency by code", description = "Retrieve currency details by code")
  @ApiResponse(responseCode = "200", description = "Currency retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Currency not found")
  public ResponseEntity<CurrencyDto> getCurrency(
      @Parameter(description = "Currency code") @PathVariable String code) {
    CurrencyDto currency = currencyService.getCurrencyByCode(code);
    return ResponseEntity.ok(currency);
  }

  // Exchange rate endpoints
  @GetMapping("/rates")
  @Operation(summary = "Get exchange rates", description = "Get exchange rates with optional filtering")
  @ApiResponse(responseCode = "200", description = "Exchange rates retrieved successfully")
  public ResponseEntity<Page<ExchangeRateDto>> getExchangeRates(
      @Parameter(description = "Base currency") @RequestParam(required = false) String baseCurrency,
      @Parameter(description = "Target currency") @RequestParam(required = false) String targetCurrency,
      @Parameter(description = "Start date") @RequestParam(required = false) LocalDate startDate,
      @Parameter(description = "End date") @RequestParam(required = false) LocalDate endDate,
      @PageableDefault(size = 20) Pageable pageable) {

    Page<ExchangeRateDto> rates = exchangeRateService.getRatesWithFilters(
        baseCurrency, targetCurrency, startDate, endDate, pageable);
    return ResponseEntity.ok(rates);
  }

  @GetMapping("/rates/{baseCurrency}/{targetCurrency}")
  @Operation(summary = "Get latest exchange rate", description = "Get the latest exchange rate between two currencies")
  @ApiResponse(responseCode = "200", description = "Exchange rate retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Exchange rate not found")
  public ResponseEntity<ExchangeRateDto> getLatestRate(
      @Parameter(description = "Base currency code") @PathVariable String baseCurrency,
      @Parameter(description = "Target currency code") @PathVariable String targetCurrency) {

    ExchangeRateDto rate = exchangeRateService.getLatestRate(baseCurrency, targetCurrency);
    return ResponseEntity.ok(rate);
  }

  @GetMapping("/rates/{baseCurrency}/{targetCurrency}/history")
  @Operation(summary = "Get historical exchange rates", description = "Get historical exchange rates between two currencies")
  @ApiResponse(responseCode = "200", description = "Historical rates retrieved successfully")
  public ResponseEntity<List<ExchangeRateDto>> getHistoricalRates(
      @Parameter(description = "Base currency code") @PathVariable String baseCurrency,
      @Parameter(description = "Target currency code") @PathVariable String targetCurrency,
      @Parameter(description = "Start date") @RequestParam LocalDate startDate,
      @Parameter(description = "End date") @RequestParam LocalDate endDate) {

    List<ExchangeRateDto> rates = exchangeRateService.getHistoricalRates(baseCurrency, targetCurrency, startDate,
        endDate);
    return ResponseEntity.ok(rates);
  }

  @PostMapping("/rates")
  @Operation(summary = "Create or update exchange rate", description = "Create a new exchange rate or update existing one")
  @ApiResponse(responseCode = "200", description = "Exchange rate created/updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @ApiResponse(responseCode = "404", description = "Currency not found")
  public ResponseEntity<ExchangeRateDto> createOrUpdateRate(
      @Valid @RequestBody ExchangeRateDto.CreateExchangeRateRequest request,
      Authentication authentication) {

    log.info("Creating/updating exchange rate: {} -> {} by user: {}",
        request.baseCurrency(), request.targetCurrency(), authentication.getName());

    ExchangeRateDto rate = exchangeRateService.createOrUpdateRate(request);
    return ResponseEntity.ok(rate);
  }

  // Currency conversion endpoints
  @PostMapping("/convert")
  @Operation(summary = "Convert currency", description = "Convert amount from one currency to another")
  @ApiResponse(responseCode = "200", description = "Currency converted successfully")
  @ApiResponse(responseCode = "400", description = "Invalid conversion request")
  @ApiResponse(responseCode = "404", description = "Exchange rate not found")
  public ResponseEntity<ConversionDto.ConversionResult> convertCurrency(
      @Valid @RequestBody ConversionDto.ConvertCurrencyRequest request,
      Authentication authentication) {

    UUID userId = UUID.fromString(authentication.getName());
    log.info("Converting {} {} to {} for user: {}",
        request.amountCents(), request.fromCurrency(), request.toCurrency(), userId);

    ConversionDto.ConversionResult result = conversionService.convertCurrency(request, userId);
    return ResponseEntity.ok(result);
  }

  @PostMapping("/rates/lookup")
  @Operation(summary = "Lookup exchange rate", description = "Get exchange rate without performing conversion")
  @ApiResponse(responseCode = "200", description = "Exchange rate lookup completed")
  public ResponseEntity<ConversionDto.ExchangeRateLookupResult> lookupExchangeRate(
      @Valid @RequestBody ConversionDto.GetExchangeRateRequest request) {

    log.debug("Looking up exchange rate: {} -> {}", request.baseCurrency(), request.targetCurrency());

    ConversionDto.ExchangeRateLookupResult result = conversionService.getExchangeRate(request);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/rates/current")
  @Operation(summary = "Get current exchange rates", description = "Get all current exchange rates (today's rates)")
  @ApiResponse(responseCode = "200", description = "Current rates retrieved successfully")
  public ResponseEntity<List<ExchangeRateDto>> getCurrentRates() {
    List<ExchangeRateDto> rates = exchangeRateService.getCurrentRates();
    return ResponseEntity.ok(rates);
  }

  @GetMapping("/rates/pairs")
  @Operation(summary = "Get available currency pairs", description = "Get all available currency pairs for conversion")
  @ApiResponse(responseCode = "200", description = "Currency pairs retrieved successfully")
  public ResponseEntity<List<String>> getAvailableCurrencyPairs() {
    List<String> pairs = exchangeRateService.getAvailableCurrencyPairs();
    return ResponseEntity.ok(pairs);
  }
}
