package com.expenses.svcfx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcfx.entity.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

  /**
   * Find all active currencies
   */
  List<Currency> findByIsActiveTrueOrderByCode();

  /**
   * Find currency by code (case-insensitive)
   */
  @Query("SELECT c FROM Currency c WHERE UPPER(c.code) = UPPER(:code)")
  Optional<Currency> findByCodeIgnoreCase(@Param("code") String code);

  /**
   * Find currencies by name containing (case-insensitive)
   */
  @Query("SELECT c FROM Currency c WHERE UPPER(c.name) LIKE UPPER(CONCAT('%', :name, '%')) AND c.isActive = true ORDER BY c.name")
  List<Currency> findByNameContainingIgnoreCase(@Param("name") String name);

  /**
   * Find active currency by code
   */
  @Query("SELECT c FROM Currency c WHERE UPPER(c.code) = UPPER(:code) AND c.isActive = true")
  Optional<Currency> findActiveByCode(@Param("code") String code);

  /**
   * Check if currency code exists
   */
  boolean existsByCodeIgnoreCase(String code);

  /**
   * Count active currencies
   */
  @Query("SELECT COUNT(c) FROM Currency c WHERE c.isActive = true")
  long countActiveCurrencies();

  /**
   * Find most commonly used currencies (for UI display)
   */
  @Query("SELECT c FROM Currency c WHERE c.code IN ('USD', 'EUR', 'GBP', 'JPY', 'BDT', 'INR') AND c.isActive = true ORDER BY c.code")
  List<Currency> findCommonCurrencies();
}
