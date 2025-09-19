package com.expenses.svcledger.repository;

import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.entity.AccountBalance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {

  /**
   * Find balance by account
   */
  Optional<AccountBalance> findByAccount(Account account);

  /**
   * Find balance by account ID
   */
  Optional<AccountBalance> findByAccountId(UUID accountId);

  /**
   * Find balances for user accounts
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'USER' AND a.ownerId = :userId AND a.isActive = true " +
      "ORDER BY a.currency")
  List<AccountBalance> findUserBalances(@Param("userId") UUID userId);

  /**
   * Find balances for group accounts
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'GROUP' AND a.ownerId = :groupId AND a.isActive = true " +
      "ORDER BY a.currency")
  List<AccountBalance> findGroupBalances(@Param("groupId") UUID groupId);

  /**
   * Find non-zero balances for a user
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'USER' AND a.ownerId = :userId AND a.isActive = true AND " +
      "ab.balanceCents != 0 " +
      "ORDER BY ABS(ab.balanceCents) DESC")
  List<AccountBalance> findNonZeroUserBalances(@Param("userId") UUID userId);

  /**
   * Find positive balances (user is owed money)
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'USER' AND a.ownerId = :userId AND a.isActive = true AND " +
      "ab.balanceCents > 0 " +
      "ORDER BY ab.balanceCents DESC")
  List<AccountBalance> findPositiveUserBalances(@Param("userId") UUID userId);

  /**
   * Find negative balances (user owes money)
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'USER' AND a.ownerId = :userId AND a.isActive = true AND " +
      "ab.balanceCents < 0 " +
      "ORDER BY ab.balanceCents ASC")
  List<AccountBalance> findNegativeUserBalances(@Param("userId") UUID userId);

  /**
   * Find balances by currency
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.currency = :currency AND a.isActive = true " +
      "ORDER BY ab.balanceCents DESC")
  List<AccountBalance> findBalancesByCurrency(@Param("currency") String currency);

  /**
   * Find balances updated after specific time
   */
  @Query("SELECT ab FROM AccountBalance ab WHERE ab.lastUpdated >= :since ORDER BY ab.lastUpdated DESC")
  List<AccountBalance> findBalancesUpdatedSince(@Param("since") ZonedDateTime since);

  /**
   * Find balances with pagination and filtering
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "(:ownerType IS NULL OR a.ownerType = :ownerType) AND " +
      "(:ownerId IS NULL OR a.ownerId = :ownerId) AND " +
      "(:currency IS NULL OR a.currency = :currency) AND " +
      "(:nonZeroOnly = false OR ab.balanceCents != 0) AND " +
      "a.isActive = true " +
      "ORDER BY ab.lastUpdated DESC")
  Page<AccountBalance> findBalancesWithFilters(@Param("ownerType") Account.OwnerType ownerType,
      @Param("ownerId") UUID ownerId,
      @Param("currency") String currency,
      @Param("nonZeroOnly") Boolean nonZeroOnly,
      Pageable pageable);

  /**
   * Get balance summary statistics
   */
  @Query("SELECT " +
      "a.currency as currency, " +
      "COUNT(ab) as accountCount, " +
      "SUM(ab.balanceCents) as totalBalance, " +
      "AVG(ab.balanceCents) as averageBalance, " +
      "COUNT(CASE WHEN ab.balanceCents > 0 THEN 1 END) as positiveCount, " +
      "COUNT(CASE WHEN ab.balanceCents < 0 THEN 1 END) as negativeCount, " +
      "COUNT(CASE WHEN ab.balanceCents = 0 THEN 1 END) as zeroCount " +
      "FROM AccountBalance ab JOIN ab.account a WHERE a.isActive = true " +
      "GROUP BY a.currency " +
      "ORDER BY a.currency")
  List<Object[]> getBalanceStatistics();

  /**
   * Find accounts with largest balances (positive or negative)
   */
  @Query("SELECT ab FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.isActive = true " +
      "ORDER BY ABS(ab.balanceCents) DESC")
  List<AccountBalance> findLargestBalances(Pageable pageable);

  /**
   * Get total balance for a group (should be zero if properly balanced)
   */
  @Query("SELECT COALESCE(SUM(ab.balanceCents), 0) FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerType = 'GROUP' AND a.ownerId = :groupId AND a.currency = :currency")
  Long getTotalGroupBalance(@Param("groupId") UUID groupId, @Param("currency") String currency);

  /**
   * Check if all accounts in a group are balanced (sum = 0)
   */
  @Query("SELECT (COALESCE(SUM(ab.balanceCents), 0) = 0) FROM AccountBalance ab JOIN ab.account a WHERE " +
      "a.ownerId = :groupId AND a.currency = :currency")
  Boolean isGroupBalanced(@Param("groupId") UUID groupId, @Param("currency") String currency);

  /**
   * Find stale balances (not updated recently)
   */
  @Query("SELECT ab FROM AccountBalance ab WHERE ab.lastUpdated < :cutoffTime ORDER BY ab.lastUpdated ASC")
  List<AccountBalance> findStaleBalances(@Param("cutoffTime") ZonedDateTime cutoffTime);
}
