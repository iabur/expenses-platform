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

import com.expenses.svcsplitengine.entity.GroupBalance;

@Repository
public interface GroupBalanceRepository extends JpaRepository<GroupBalance, UUID> {

  /**
   * Find balance for specific user in group and currency
   */
  Optional<GroupBalance> findByGroupIdAndUserIdAndCurrency(
      UUID groupId,
      UUID userId,
      String currency);

  /**
   * Find all balances for a group
   */
  List<GroupBalance> findByGroupIdOrderByBalanceCentsDesc(UUID groupId);

  /**
   * Find all balances for a group in specific currency
   */
  List<GroupBalance> findByGroupIdAndCurrencyOrderByBalanceCentsDesc(
      UUID groupId,
      String currency);

  /**
   * Find all balances for a user across groups
   */
  List<GroupBalance> findByUserIdOrderByLastUpdatedDesc(UUID userId);

  /**
   * Find all balances for a user across groups (paginated)
   */
  Page<GroupBalance> findByUserIdOrderByBalanceCentsDesc(UUID userId, Pageable pageable);

  /**
   * Find users who owe money in a group
   */
  @Query("SELECT gb FROM GroupBalance gb WHERE gb.groupId = :groupId " +
      "AND gb.balanceCents < 0 ORDER BY gb.balanceCents ASC")
  List<GroupBalance> findDebtorsInGroup(@Param("groupId") UUID groupId);

  /**
   * Find users who are owed money in a group
   */
  @Query("SELECT gb FROM GroupBalance gb WHERE gb.groupId = :groupId " +
      "AND gb.balanceCents > 0 ORDER BY gb.balanceCents DESC")
  List<GroupBalance> findCreditorsInGroup(@Param("groupId") UUID groupId);

  /**
   * Find users with non-zero balances in a group
   */
  @Query("SELECT gb FROM GroupBalance gb WHERE gb.groupId = :groupId " +
      "AND gb.balanceCents != 0 ORDER BY ABS(gb.balanceCents) DESC")
  List<GroupBalance> findNonZeroBalancesInGroup(@Param("groupId") UUID groupId);

  /**
   * Get total group debt (sum of all negative balances)
   */
  @Query("SELECT COALESCE(SUM(ABS(gb.balanceCents)), 0) FROM GroupBalance gb " +
      "WHERE gb.groupId = :groupId AND gb.balanceCents < 0 AND gb.currency = :currency")
  Long getTotalGroupDebt(@Param("groupId") UUID groupId, @Param("currency") String currency);

  /**
   * Get total group credit (sum of all positive balances)
   */
  @Query("SELECT COALESCE(SUM(gb.balanceCents), 0) FROM GroupBalance gb " +
      "WHERE gb.groupId = :groupId AND gb.balanceCents > 0 AND gb.currency = :currency")
  Long getTotalGroupCredit(@Param("groupId") UUID groupId, @Param("currency") String currency);

  /**
   * Check if group is balanced (all balances sum to zero)
   */
  @Query("SELECT ABS(COALESCE(SUM(gb.balanceCents), 0)) < 100 FROM GroupBalance gb " +
      "WHERE gb.groupId = :groupId AND gb.currency = :currency")
  boolean isGroupBalanced(@Param("groupId") UUID groupId, @Param("currency") String currency);

  /**
   * Find balances that need settlement (non-zero balances above threshold)
   */
  @Query("SELECT gb FROM GroupBalance gb WHERE gb.groupId = :groupId " +
      "AND ABS(gb.balanceCents) >= :thresholdCents ORDER BY ABS(gb.balanceCents) DESC")
  List<GroupBalance> findBalancesForSettlement(
      @Param("groupId") UUID groupId,
      @Param("thresholdCents") Long thresholdCents);
}
