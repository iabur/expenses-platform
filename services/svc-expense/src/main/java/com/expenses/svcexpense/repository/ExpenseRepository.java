package com.expenses.svcexpense.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcexpense.entity.Expense;
import com.expenses.svcexpense.service.ExpenseService.CategoryTotal;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

  /**
   * Find expense by ID (not deleted)
   */
  Optional<Expense> findByIdAndIsDeletedFalse(UUID id);

  /**
   * Find expenses by group ID
   */
  Page<Expense> findByGroupIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

  /**
   * Find expenses by creator
   */
  Page<Expense> findByCreatorIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

  /**
   * Find expenses where user is a participant
   */
  @Query("SELECT e FROM Expense e JOIN e.participants p WHERE p.userId = :userId " +
      "AND e.isDeleted = false ORDER BY e.createdAt DESC")
  Page<Expense> findByParticipantsUserIdAndIsDeletedFalseOrderByCreatedAtDesc(
      @Param("userId") UUID userId, Pageable pageable);

  /**
   * Search expenses in a group by category or note
   */
  @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId AND e.isDeleted = false " +
      "AND (LOWER(e.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
      "OR LOWER(e.note) LIKE LOWER(CONCAT('%', :query, '%'))) " +
      "ORDER BY e.createdAt DESC")
  Page<Expense> searchInGroup(@Param("query") String query,
      @Param("groupId") UUID groupId,
      Pageable pageable);

  /**
   * Search user's expenses by category or note
   */
  @Query("SELECT e FROM Expense e WHERE e.isDeleted = false " +
      "AND (e.creatorId = :userId OR EXISTS (SELECT p FROM e.participants p WHERE p.userId = :userId)) " +
      "AND (LOWER(e.category) LIKE LOWER(CONCAT('%', :query, '%')) " +
      "OR LOWER(e.note) LIKE LOWER(CONCAT('%', :query, '%'))) " +
      "ORDER BY e.createdAt DESC")
  Page<Expense> searchUserExpenses(@Param("query") String query,
      @Param("userId") UUID userId,
      Pageable pageable);

  /**
   * Count expenses in a group
   */
  Long countByGroupIdAndIsDeletedFalse(UUID groupId);

  /**
   * Sum total amount for a group
   */
  @Query("SELECT COALESCE(SUM(e.amountCents), 0) FROM Expense e " +
      "WHERE e.groupId = :groupId AND e.isDeleted = false")
  Long sumAmountCentsByGroupId(@Param("groupId") UUID groupId);

  /**
   * Sum total amount for a group (as BigDecimal)
   */
  default BigDecimal sumAmountByGroupId(UUID groupId) {
    Long amountCents = sumAmountCentsByGroupId(groupId);
    return BigDecimal.valueOf(amountCents, 2);
  }

  /**
   * Get category totals for a group
   */
  @Query("SELECT new com.expenses.svcexpense.service.ExpenseService$CategoryTotal(" +
      "e.category, COUNT(e), CAST(SUM(e.amountCents) as java.math.BigDecimal)) " +
      "FROM Expense e WHERE e.groupId = :groupId AND e.isDeleted = false " +
      "GROUP BY e.category ORDER BY SUM(e.amountCents) DESC")
  List<CategoryTotal> getCategoryTotalsCents(@Param("groupId") UUID groupId);

  /**
   * Get category totals for a group (as BigDecimal)
   */
  default List<CategoryTotal> getCategoryTotals(UUID groupId) {
    return getCategoryTotalsCents(groupId).stream()
        .map(ct -> new CategoryTotal(
            ct.category(),
            ct.count(),
            BigDecimal.valueOf(ct.totalAmount().longValue(), 2)))
        .toList();
  }

  /**
   * Find recent expenses for a user (across all groups)
   */
  @Query("SELECT e FROM Expense e WHERE e.isDeleted = false " +
      "AND (e.creatorId = :userId OR EXISTS (SELECT p FROM e.participants p WHERE p.userId = :userId)) " +
      "ORDER BY e.createdAt DESC")
  Page<Expense> findRecentUserExpenses(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Find expenses by multiple IDs
   */
  @Query("SELECT e FROM Expense e WHERE e.id IN :expenseIds AND e.isDeleted = false")
  List<Expense> findByIdInAndIsDeletedFalse(@Param("expenseIds") List<UUID> expenseIds);

  /**
   * Check if user has access to expense
   */
  @Query("SELECT COUNT(e) > 0 FROM Expense e WHERE e.id = :expenseId AND e.isDeleted = false " +
      "AND (e.creatorId = :userId OR EXISTS (SELECT p FROM e.participants p WHERE p.userId = :userId))")
  boolean hasUserAccess(@Param("expenseId") UUID expenseId, @Param("userId") UUID userId);

  /**
   * Find expenses by date range
   */
  @Query("SELECT e FROM Expense e WHERE e.groupId = :groupId AND e.isDeleted = false " +
      "AND e.occurredAt >= :startDate AND e.occurredAt <= :endDate " +
      "ORDER BY e.occurredAt DESC")
  List<Expense> findByGroupIdAndDateRange(@Param("groupId") UUID groupId,
      @Param("startDate") java.time.LocalDate startDate,
      @Param("endDate") java.time.LocalDate endDate);

  /**
   * Get monthly totals for a group
   */
  @Query("SELECT EXTRACT(YEAR FROM e.occurredAt) as year, " +
      "EXTRACT(MONTH FROM e.occurredAt) as month, " +
      "SUM(e.amountCents) as total " +
      "FROM Expense e WHERE e.groupId = :groupId AND e.isDeleted = false " +
      "GROUP BY EXTRACT(YEAR FROM e.occurredAt), EXTRACT(MONTH FROM e.occurredAt) " +
      "ORDER BY year DESC, month DESC")
  List<Object[]> getMonthlyTotals(@Param("groupId") UUID groupId);
}
