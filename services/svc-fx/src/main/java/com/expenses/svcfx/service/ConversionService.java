package com.expenses.svcfx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcfx.dto.ConversionDto;
import com.expenses.svcfx.entity.ConversionHistory;
import com.expenses.svcfx.entity.Currency;
import com.expenses.svcfx.entity.ExchangeRate;
import com.expenses.svcfx.exception.CurrencyConversionException;
import com.expenses.svcfx.exception.CurrencyNotFoundException;
import com.expenses.svcfx.exception.ExchangeRateNotFoundException;
import com.expenses.svcfx.repository.ConversionHistoryRepository;
import com.expenses.svcfx.repository.CurrencyRepository;
import com.expenses.svcfx.repository.ExchangeRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ConversionService {

  private final ExchangeRateRepository exchangeRateRepository;
  private final CurrencyRepository currencyRepository;
  private final ConversionHistoryRepository conversionHistoryRepository;

  /**
   * Convert currency amount and save to history
   */
  public ConversionDto.ConversionResult convertCurrency(ConversionDto.ConvertCurrencyRequest request, UUID userId) {
    log.info("Converting {} cents from {} to {} for user {}",
        request.amountCents(), request.fromCurrency(), request.toCurrency(), userId);

    // Validate currencies
    Currency fromCurrency = currencyRepository.findActiveByCode(request.fromCurrency())
        .orElseThrow(() -> new CurrencyNotFoundException("From currency not found: " + request.fromCurrency()));

    Currency toCurrency = currencyRepository.findActiveByCode(request.toCurrency())
        .orElseThrow(() -> new CurrencyNotFoundException("To currency not found: " + request.toCurrency()));

    // Handle same currency conversion
    if (request.fromCurrency().equals(request.toCurrency())) {
      log.debug("Same currency conversion, no rate needed");

      // Still save to history for tracking
      ConversionHistory history = ConversionHistory.builder()
          .fromCurrency(fromCurrency)
          .toCurrency(toCurrency)
          .originalAmountCents(request.amountCents())
          .convertedAmountCents(request.amountCents())
          .exchangeRate(BigDecimal.ONE)
          .userId(userId)
          .referenceType(request.referenceType())
          .referenceId(request.referenceId())
          .build();

      conversionHistoryRepository.save(history);

      return ConversionDto.ConversionResult.create(
          request.fromCurrency(),
          request.toCurrency(),
          request.amountCents(),
          request.amountCents(),
          BigDecimal.ONE,
          LocalDate.now());
    }

    // Find exchange rate
    ExchangeRate exchangeRate = exchangeRateRepository.findLatestRate(request.fromCurrency(), request.toCurrency())
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s", request.fromCurrency(), request.toCurrency())));

    // Convert amount
    long convertedAmountCents;
    try {
      BigDecimal originalAmount = BigDecimal.valueOf(request.amountCents());
      BigDecimal convertedAmount = originalAmount.multiply(exchangeRate.getRate());
      convertedAmountCents = convertedAmount.longValue();
    } catch (Exception e) {
      throw new CurrencyConversionException("Error converting currency: " + e.getMessage(), e);
    }

    // Save conversion history
    ConversionHistory history = ConversionHistory.builder()
        .fromCurrency(fromCurrency)
        .toCurrency(toCurrency)
        .originalAmountCents(request.amountCents())
        .convertedAmountCents(convertedAmountCents)
        .exchangeRate(exchangeRate.getRate())
        .userId(userId)
        .referenceType(request.referenceType())
        .referenceId(request.referenceId())
        .build();

    ConversionHistory savedHistory = conversionHistoryRepository.save(history);

    log.info("Converted {} {} to {} {} using rate {} for user {}",
        request.amountCents(), request.fromCurrency(),
        convertedAmountCents, request.toCurrency(),
        exchangeRate.getRate(), userId);

    return ConversionDto.ConversionResult.create(
        request.fromCurrency(),
        request.toCurrency(),
        request.amountCents(),
        convertedAmountCents,
        exchangeRate.getRate(),
        exchangeRate.getRateDate());
  }

  /**
   * Get exchange rate without conversion
   */
  @Transactional(readOnly = true)
  public ConversionDto.ExchangeRateLookupResult getExchangeRate(ConversionDto.GetExchangeRateRequest request) {
    log.debug("Looking up exchange rate for {} -> {}", request.baseCurrency(), request.targetCurrency());

    try {
      LocalDate targetDate = request.rateDate() != null ? request.rateDate() : LocalDate.now();

      ExchangeRate exchangeRate = exchangeRateRepository.findByDateAndCurrencies(
          request.baseCurrency(), request.targetCurrency(), targetDate)
          .or(() -> exchangeRateRepository.findLatestRate(request.baseCurrency(), request.targetCurrency()))
          .orElse(null);

      if (exchangeRate == null) {
        return ConversionDto.ExchangeRateLookupResult.builder()
            .baseCurrency(request.baseCurrency())
            .targetCurrency(request.targetCurrency())
            .found(false)
            .errorMessage("Exchange rate not found")
            .build();
      }

      return ConversionDto.ExchangeRateLookupResult.builder()
          .baseCurrency(request.baseCurrency())
          .targetCurrency(request.targetCurrency())
          .rate(exchangeRate.getRate())
          .rateDate(exchangeRate.getRateDate())
          .source(exchangeRate.getSource())
          .found(true)
          .build();

    } catch (Exception e) {
      log.error("Error looking up exchange rate", e);

      return ConversionDto.ExchangeRateLookupResult.builder()
          .baseCurrency(request.baseCurrency())
          .targetCurrency(request.targetCurrency())
          .found(false)
          .errorMessage("Error: " + e.getMessage())
          .build();
    }
  }

  /**
   * Convert amount in cents using latest exchange rate
   */
  @Transactional(readOnly = true)
  public long convertAmountCents(String fromCurrency, String toCurrency, long amountCents) {
    log.debug("Converting {} cents from {} to {}", amountCents, fromCurrency, toCurrency);

    // Same currency, no conversion needed
    if (fromCurrency.equals(toCurrency)) {
      return amountCents;
    }

    ExchangeRate rate = exchangeRateRepository.findLatestRate(fromCurrency, toCurrency)
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s", fromCurrency, toCurrency)));

    BigDecimal amount = BigDecimal.valueOf(amountCents);
    BigDecimal convertedAmount = amount.multiply(rate.getRate());

    return convertedAmount.longValue();
  }
}
