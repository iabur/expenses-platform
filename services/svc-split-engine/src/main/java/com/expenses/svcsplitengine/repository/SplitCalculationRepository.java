package com.expenses.svcsplitengine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcsplitengine.entity.SplitCalculation;

@Repository
public interface SplitCalculationRepository extends JpaRepository<SplitCalculation, UUID> {

  /**
   * Find split calculation by expense ID and status
   */
  Optional<SplitCalculation> findByExpenseIdAndCalculationStatus(
      UUID expenseId,
      SplitCalculation.CalculationStatus status);

  /**
   * Find latest split calculation for expense
   */
  @Query("SELECT sc FROM SplitCalculation sc WHERE sc.expenseId = :expenseId " +
      "ORDER BY sc.createdAt DESC LIMIT 1")
  Optional<SplitCalculation> findLatestByExpenseId(@Param("expenseId") UUID expenseId);

  /**
   * Find all split calculations for a group
   */
  List<SplitCalculation> findByGroupIdOrderByCreatedAtDesc(UUID groupId);

  /**
   * Find split calculations by group with pagination
   */
  Page<SplitCalculation> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

  /**
   * Find split calculations by status
   */
  List<SplitCalculation> findByCalculationStatusOrderByCreatedAtDesc(
      SplitCalculation.CalculationStatus status);

  /**
   * Find failed split calculations for retry
   */
  @Query("SELECT sc FROM SplitCalculation sc WHERE sc.calculationStatus = 'FAILED' " +
      "AND sc.updatedAt < :retryAfter ORDER BY sc.updatedAt ASC")
  List<SplitCalculation> findFailedCalculationsForRetry(
      @Param("retryAfter") java.time.ZonedDateTime retryAfter);

  /**
   * Count split calculations by group and status
   */
  long countByGroupIdAndCalculationStatus(UUID groupId, SplitCalculation.CalculationStatus status);

  /**
   * Check if expense has successful split calculation
   */
  @Query("SELECT COUNT(sc) > 0 FROM SplitCalculation sc WHERE sc.expenseId = :expenseId " +
      "AND sc.calculationStatus = 'CALCULATED'")
  boolean hasSuccessfulSplitCalculation(@Param("expenseId") UUID expenseId);

  /**
   * Find split calculations for multiple expenses
   */
  @Query("SELECT sc FROM SplitCalculation sc WHERE sc.expenseId IN :expenseIds " +
      "AND sc.calculationStatus = 'CALCULATED'")
  List<SplitCalculation> findByExpenseIdsAndCalculated(@Param("expenseIds") List<UUID> expenseIds);
}
