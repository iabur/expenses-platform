package com.expenses.svcfx.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcfx.entity.ConversionHistory;

@Repository
public interface ConversionHistoryRepository extends JpaRepository<ConversionHistory, UUID> {

  /**
   * Find conversion history for a user
   */
  Page<ConversionHistory> findByUserIdOrderByConversionDateDesc(UUID userId, Pageable pageable);

  /**
   * Find conversion history by reference (expense, settlement, etc.)
   */
  List<ConversionHistory> findByReferenceTypeAndReferenceIdOrderByConversionDateDesc(
      String referenceType, UUID referenceId);

  /**
   * Find conversions for specific currency pair
   */
  @Query("SELECT ch FROM ConversionHistory ch WHERE " +
      "ch.fromCurrency.code = :fromCurrency AND " +
      "ch.toCurrency.code = :toCurrency " +
      "ORDER BY ch.conversionDate DESC")
  Page<ConversionHistory> findByCurrencyPair(@Param("fromCurrency") String fromCurrency,
      @Param("toCurrency") String toCurrency,
      Pageable pageable);

  /**
   * Find recent conversions (last N days)
   */
  @Query("SELECT ch FROM ConversionHistory ch WHERE " +
      "ch.conversionDate >= :since " +
      "ORDER BY ch.conversionDate DESC")
  List<ConversionHistory> findRecentConversions(@Param("since") ZonedDateTime since);

  /**
   * Find conversions for a user within date range
   */
  @Query("SELECT ch FROM ConversionHistory ch WHERE " +
      "ch.userId = :userId AND " +
      "ch.conversionDate BETWEEN :startDate AND :endDate " +
      "ORDER BY ch.conversionDate DESC")
  List<ConversionHistory> findByUserAndDateRange(@Param("userId") UUID userId,
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate);

  /**
   * Get conversion statistics for a user
   */
  @Query("SELECT " +
      "ch.fromCurrency.code as fromCurrency, " +
      "ch.toCurrency.code as toCurrency, " +
      "COUNT(ch) as conversionCount, " +
      "SUM(ch.originalAmountCents) as totalOriginalAmount, " +
      "SUM(ch.convertedAmountCents) as totalConvertedAmount, " +
      "AVG(ch.exchangeRate) as averageRate " +
      "FROM ConversionHistory ch WHERE " +
      "ch.userId = :userId " +
      "GROUP BY ch.fromCurrency.code, ch.toCurrency.code " +
      "ORDER BY conversionCount DESC")
  List<Object[]> getConversionStatsByUser(@Param("userId") UUID userId);

  /**
   * Find most popular currency conversions
   */
  @Query("SELECT " +
      "ch.fromCurrency.code as fromCurrency, " +
      "ch.toCurrency.code as toCurrency, " +
      "COUNT(ch) as conversionCount " +
      "FROM ConversionHistory ch " +
      "WHERE ch.conversionDate >= :since " +
      "GROUP BY ch.fromCurrency.code, ch.toCurrency.code " +
      "ORDER BY conversionCount DESC")
  List<Object[]> getMostPopularConversions(@Param("since") ZonedDateTime since, Pageable pageable);

  /**
   * Count conversions for a user
   */
  long countByUserId(UUID userId);

  /**
   * Count conversions by reference
   */
  long countByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);

  /**
   * Find total converted amount for user in specific currency
   */
  @Query("SELECT COALESCE(SUM(ch.convertedAmountCents), 0) FROM ConversionHistory ch WHERE " +
      "ch.userId = :userId AND " +
      "ch.toCurrency.code = :currency AND " +
      "ch.conversionDate >= :since")
  Long getTotalConvertedAmount(@Param("userId") UUID userId,
      @Param("currency") String currency,
      @Param("since") ZonedDateTime since);

  /**
   * Delete old conversion history (for cleanup)
   */
  @Query("DELETE FROM ConversionHistory ch WHERE ch.conversionDate < :cutoffDate")
  void deleteOldConversions(@Param("cutoffDate") ZonedDateTime cutoffDate);
}
