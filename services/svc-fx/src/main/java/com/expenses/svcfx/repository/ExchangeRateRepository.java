package com.expenses.svcfx.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcfx.entity.ExchangeRate;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, UUID> {

  /**
   * Find the latest exchange rate between two currencies
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.baseCurrency.code = :baseCurrency AND " +
      "er.targetCurrency.code = :targetCurrency AND " +
      "er.isActive = true " +
      "ORDER BY er.rateDate DESC, er.createdAt DESC")
  Optional<ExchangeRate> findLatestRate(@Param("baseCurrency") String baseCurrency,
      @Param("targetCurrency") String targetCurrency);

  /**
   * Find exchange rate for specific date
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.baseCurrency.code = :baseCurrency AND " +
      "er.targetCurrency.code = :targetCurrency AND " +
      "er.rateDate = :date AND " +
      "er.isActive = true")
  Optional<ExchangeRate> findByDateAndCurrencies(@Param("baseCurrency") String baseCurrency,
      @Param("targetCurrency") String targetCurrency,
      @Param("date") LocalDate date);

  /**
   * Find all rates for a base currency on a specific date
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.baseCurrency.code = :baseCurrency AND " +
      "er.rateDate = :date AND " +
      "er.isActive = true " +
      "ORDER BY er.targetCurrency.code")
  List<ExchangeRate> findByBaseCurrencyAndDate(@Param("baseCurrency") String baseCurrency,
      @Param("date") LocalDate date);

  /**
   * Find historical rates between two currencies
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.baseCurrency.code = :baseCurrency AND " +
      "er.targetCurrency.code = :targetCurrency AND " +
      "er.rateDate BETWEEN :startDate AND :endDate AND " +
      "er.isActive = true " +
      "ORDER BY er.rateDate DESC")
  List<ExchangeRate> findHistoricalRates(@Param("baseCurrency") String baseCurrency,
      @Param("targetCurrency") String targetCurrency,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * Find rates by source (e.g., 'MANUAL', 'API', 'BANK')
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.source = :source AND " +
      "er.rateDate = :date AND " +
      "er.isActive = true " +
      "ORDER BY er.baseCurrency.code, er.targetCurrency.code")
  List<ExchangeRate> findBySourceAndDate(@Param("source") String source, @Param("date") LocalDate date);

  /**
   * Find all current rates (today's date)
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.rateDate = CURRENT_DATE AND " +
      "er.isActive = true " +
      "ORDER BY er.baseCurrency.code, er.targetCurrency.code")
  List<ExchangeRate> findCurrentRates();

  /**
   * Find rates that need updating (older than specified days)
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "er.rateDate < :cutoffDate AND " +
      "er.isActive = true " +
      "GROUP BY er.baseCurrency.code, er.targetCurrency.code " +
      "ORDER BY er.baseCurrency.code, er.targetCurrency.code")
  List<ExchangeRate> findStaleRates(@Param("cutoffDate") LocalDate cutoffDate);

  /**
   * Check if rate exists for currency pair and date
   */
  @Query("SELECT COUNT(er) > 0 FROM ExchangeRate er WHERE " +
      "er.baseCurrency.code = :baseCurrency AND " +
      "er.targetCurrency.code = :targetCurrency AND " +
      "er.rateDate = :date")
  boolean existsByDateAndCurrencies(@Param("baseCurrency") String baseCurrency,
      @Param("targetCurrency") String targetCurrency,
      @Param("date") LocalDate date);

  /**
   * Find paginated rates with filtering
   */
  @Query("SELECT er FROM ExchangeRate er WHERE " +
      "(:baseCurrency IS NULL OR er.baseCurrency.code = :baseCurrency) AND " +
      "(:targetCurrency IS NULL OR er.targetCurrency.code = :targetCurrency) AND " +
      "(:startDate IS NULL OR er.rateDate >= :startDate) AND " +
      "(:endDate IS NULL OR er.rateDate <= :endDate) AND " +
      "er.isActive = true " +
      "ORDER BY er.rateDate DESC, er.baseCurrency.code, er.targetCurrency.code")
  Page<ExchangeRate> findRatesWithFilters(@Param("baseCurrency") String baseCurrency,
      @Param("targetCurrency") String targetCurrency,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable);

  /**
   * Get available currency pairs
   */
  @Query("SELECT DISTINCT CONCAT(er.baseCurrency.code, '/', er.targetCurrency.code) " +
      "FROM ExchangeRate er WHERE er.isActive = true ORDER BY 1")
  List<String> findAvailableCurrencyPairs();
}
