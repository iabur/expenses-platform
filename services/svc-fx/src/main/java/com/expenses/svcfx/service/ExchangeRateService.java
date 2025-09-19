package com.expenses.svcfx.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcfx.dto.ExchangeRateDto;
import com.expenses.svcfx.entity.Currency;
import com.expenses.svcfx.entity.ExchangeRate;
import com.expenses.svcfx.exception.CurrencyNotFoundException;
import com.expenses.svcfx.exception.ExchangeRateNotFoundException;
import com.expenses.svcfx.repository.CurrencyRepository;
import com.expenses.svcfx.repository.ExchangeRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

  private final ExchangeRateRepository exchangeRateRepository;
  private final CurrencyRepository currencyRepository;

  /**
   * Get latest exchange rate between two currencies
   */
  @Transactional(readOnly = true)
  public ExchangeRateDto getLatestRate(String baseCurrency, String targetCurrency) {
    log.debug("Getting latest rate for {} -> {}", baseCurrency, targetCurrency);

    ExchangeRate exchangeRate = exchangeRateRepository.findLatestRate(baseCurrency, targetCurrency)
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s", baseCurrency, targetCurrency)));

    return ExchangeRateDto.from(exchangeRate);
  }

  /**
   * Get exchange rate for specific date
   */
  @Transactional(readOnly = true)
  public ExchangeRateDto getRateForDate(String baseCurrency, String targetCurrency, LocalDate date) {
    log.debug("Getting rate for {} -> {} on {}", baseCurrency, targetCurrency, date);

    ExchangeRate exchangeRate = exchangeRateRepository.findByDateAndCurrencies(baseCurrency, targetCurrency, date)
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s on %s", baseCurrency, targetCurrency, date)));

    return ExchangeRateDto.from(exchangeRate);
  }

  /**
   * Create or update exchange rate
   */
  public ExchangeRateDto createOrUpdateRate(ExchangeRateDto.CreateExchangeRateRequest request) {
    log.info("Creating/updating exchange rate: {} -> {} = {} on {}",
        request.baseCurrency(), request.targetCurrency(), request.rate(), request.rateDate());

    // Validate currencies exist
    Currency baseCurrency = currencyRepository.findActiveByCode(request.baseCurrency())
        .orElseThrow(() -> new CurrencyNotFoundException("Base currency not found: " + request.baseCurrency()));

    Currency targetCurrency = currencyRepository.findActiveByCode(request.targetCurrency())
        .orElseThrow(() -> new CurrencyNotFoundException("Target currency not found: " + request.targetCurrency()));

    // Check if rate already exists for this date
    ExchangeRate exchangeRate = exchangeRateRepository.findByDateAndCurrencies(
        request.baseCurrency(), request.targetCurrency(), request.rateDate())
        .orElse(null);

    if (exchangeRate != null) {
      // Update existing rate
      exchangeRate.setRate(request.rate());
      exchangeRate.setSource(request.source());
    } else {
      // Create new rate
      exchangeRate = ExchangeRate.builder()
          .baseCurrency(baseCurrency)
          .targetCurrency(targetCurrency)
          .rate(request.rate())
          .rateDate(request.rateDate())
          .source(request.source())
          .isActive(true)
          .build();
    }

    ExchangeRate savedRate = exchangeRateRepository.save(exchangeRate);

    log.info("Saved exchange rate: {} -> {} = {} on {}",
        savedRate.getBaseCurrency().getCode(),
        savedRate.getTargetCurrency().getCode(),
        savedRate.getRate(),
        savedRate.getRateDate());

    return ExchangeRateDto.from(savedRate);
  }

  /**
   * Get all current rates (today's rates)
   */
  @Transactional(readOnly = true)
  public List<ExchangeRateDto> getCurrentRates() {
    log.debug("Fetching all current exchange rates");

    return exchangeRateRepository.findCurrentRates()
        .stream()
        .map(ExchangeRateDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Get rates with filtering and pagination
   */
  @Transactional(readOnly = true)
  public Page<ExchangeRateDto> getRatesWithFilters(String baseCurrency, String targetCurrency,
      LocalDate startDate, LocalDate endDate,
      Pageable pageable) {
    log.debug("Fetching rates with filters - base: {}, target: {}, start: {}, end: {}",
        baseCurrency, targetCurrency, startDate, endDate);

    return exchangeRateRepository.findRatesWithFilters(baseCurrency, targetCurrency, startDate, endDate, pageable)
        .map(ExchangeRateDto::from);
  }

  /**
   * Get historical rates between two currencies
   */
  @Transactional(readOnly = true)
  public List<ExchangeRateDto> getHistoricalRates(String baseCurrency, String targetCurrency,
      LocalDate startDate, LocalDate endDate) {
    log.debug("Fetching historical rates for {} -> {} from {} to {}",
        baseCurrency, targetCurrency, startDate, endDate);

    return exchangeRateRepository.findHistoricalRates(baseCurrency, targetCurrency, startDate, endDate)
        .stream()
        .map(ExchangeRateDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Get available currency pairs
   */
  @Transactional(readOnly = true)
  public List<String> getAvailableCurrencyPairs() {
    log.debug("Fetching available currency pairs");
    return exchangeRateRepository.findAvailableCurrencyPairs();
  }

  /**
   * Convert amount using latest exchange rate
   */
  @Transactional(readOnly = true)
  public BigDecimal convertAmount(String fromCurrency, String toCurrency, BigDecimal amount) {
    log.debug("Converting {} {} to {}", amount, fromCurrency, toCurrency);

    // Same currency, no conversion needed
    if (fromCurrency.equals(toCurrency)) {
      return amount;
    }

    ExchangeRate rate = exchangeRateRepository.findLatestRate(fromCurrency, toCurrency)
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s", fromCurrency, toCurrency)));

    BigDecimal convertedAmount = amount.multiply(rate.getRate());

    log.debug("Converted {} {} to {} {} using rate {}",
        amount, fromCurrency, convertedAmount, toCurrency, rate.getRate());

    return convertedAmount;
  }

  /**
   * Deactivate exchange rate
   */
  public void deactivateRate(String baseCurrency, String targetCurrency, LocalDate date) {
    log.info("Deactivating rate for {} -> {} on {}", baseCurrency, targetCurrency, date);

    ExchangeRate exchangeRate = exchangeRateRepository.findByDateAndCurrencies(baseCurrency, targetCurrency, date)
        .orElseThrow(() -> new ExchangeRateNotFoundException(
            String.format("Exchange rate not found for %s -> %s on %s", baseCurrency, targetCurrency, date)));

    exchangeRate.deactivate();
    exchangeRateRepository.save(exchangeRate);

    log.info("Deactivated exchange rate for {} -> {} on {}", baseCurrency, targetCurrency, date);
  }
}
