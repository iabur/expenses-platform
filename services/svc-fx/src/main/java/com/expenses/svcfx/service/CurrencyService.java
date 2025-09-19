package com.expenses.svcfx.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcfx.dto.CurrencyDto;
import com.expenses.svcfx.entity.Currency;
import com.expenses.svcfx.exception.CurrencyAlreadyExistsException;
import com.expenses.svcfx.exception.CurrencyNotFoundException;
import com.expenses.svcfx.repository.CurrencyRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

  private final CurrencyRepository currencyRepository;

  /**
   * Get all active currencies
   */
  @Transactional(readOnly = true)
  public List<CurrencyDto> getAllActiveCurrencies() {
    log.debug("Fetching all active currencies");

    return currencyRepository.findByIsActiveTrueOrderByCode()
        .stream()
        .map(CurrencyDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Get currency by code
   */
  @Transactional(readOnly = true)
  public CurrencyDto getCurrencyByCode(String code) {
    log.debug("Fetching currency by code: {}", code);

    Currency currency = currencyRepository.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + code));

    return CurrencyDto.from(currency);
  }

  /**
   * Get active currency by code
   */
  @Transactional(readOnly = true)
  public CurrencyDto getActiveCurrencyByCode(String code) {
    log.debug("Fetching active currency by code: {}", code);

    Currency currency = currencyRepository.findActiveByCode(code)
        .orElseThrow(() -> new CurrencyNotFoundException("Active currency not found: " + code));

    return CurrencyDto.from(currency);
  }

  /**
   * Search currencies by name
   */
  @Transactional(readOnly = true)
  public List<CurrencyDto> searchCurrenciesByName(String name) {
    log.debug("Searching currencies by name: {}", name);

    return currencyRepository.findByNameContainingIgnoreCase(name)
        .stream()
        .map(CurrencyDto::from)
        .collect(Collectors.toList());
  }

  /**
   * Get common currencies (for UI dropdowns)
   */
  @Transactional(readOnly = true)
  public List<CurrencyDto.CurrencyOption> getCommonCurrencies() {
    log.debug("Fetching common currencies");

    return currencyRepository.findCommonCurrencies()
        .stream()
        .map(CurrencyDto.CurrencyOption::from)
        .collect(Collectors.toList());
  }

  /**
   * Create new currency
   */
  public CurrencyDto createCurrency(CurrencyDto.CreateCurrencyRequest request) {
    log.info("Creating new currency: {}", request.code());

    // Check if currency already exists
    if (currencyRepository.existsByCodeIgnoreCase(request.code())) {
      throw new CurrencyAlreadyExistsException("Currency already exists: " + request.code());
    }

    Currency currency = request.toEntity();
    Currency savedCurrency = currencyRepository.save(currency);

    log.info("Created currency: {} - {}", savedCurrency.getCode(), savedCurrency.getName());

    return CurrencyDto.from(savedCurrency);
  }

  /**
   * Update currency
   */
  public CurrencyDto updateCurrency(String code, CurrencyDto.UpdateCurrencyRequest request) {
    log.info("Updating currency: {}", code);

    Currency currency = currencyRepository.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + code));

    request.updateEntity(currency);
    Currency updatedCurrency = currencyRepository.save(currency);

    log.info("Updated currency: {} - {}", updatedCurrency.getCode(), updatedCurrency.getName());

    return CurrencyDto.from(updatedCurrency);
  }

  /**
   * Activate currency
   */
  public CurrencyDto activateCurrency(String code) {
    log.info("Activating currency: {}", code);

    Currency currency = currencyRepository.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + code));

    currency.activate();
    Currency activatedCurrency = currencyRepository.save(currency);

    log.info("Activated currency: {}", activatedCurrency.getCode());

    return CurrencyDto.from(activatedCurrency);
  }

  /**
   * Deactivate currency
   */
  public CurrencyDto deactivateCurrency(String code) {
    log.info("Deactivating currency: {}", code);

    Currency currency = currencyRepository.findByCodeIgnoreCase(code)
        .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + code));

    currency.deactivate();
    Currency deactivatedCurrency = currencyRepository.save(currency);

    log.info("Deactivated currency: {}", deactivatedCurrency.getCode());

    return CurrencyDto.from(deactivatedCurrency);
  }

  /**
   * Check if currency exists and is active
   */
  @Transactional(readOnly = true)
  public boolean isActiveCurrency(String code) {
    return currencyRepository.findActiveByCode(code).isPresent();
  }

  /**
   * Get currency statistics
   */
  @Transactional(readOnly = true)
  public CurrencyStats getCurrencyStats() {
    long totalCurrencies = currencyRepository.count();
    long activeCurrencies = currencyRepository.countActiveCurrencies();

    return new CurrencyStats(totalCurrencies, activeCurrencies);
  }

  /**
   * Validate currency code format
   */
  public void validateCurrencyCode(String code) {
    if (code == null || code.trim().isEmpty()) {
      throw new IllegalArgumentException("Currency code cannot be null or empty");
    }

    if (code.length() != 3) {
      throw new IllegalArgumentException("Currency code must be exactly 3 characters");
    }

    if (!code.matches("^[A-Z]{3}$")) {
      throw new IllegalArgumentException("Currency code must contain only uppercase letters");
    }
  }

  // Helper record for statistics
  public record CurrencyStats(long totalCurrencies, long activeCurrencies) {
  }
}
