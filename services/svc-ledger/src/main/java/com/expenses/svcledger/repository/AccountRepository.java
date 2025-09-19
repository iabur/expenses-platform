package com.expenses.svcledger.repository;

import com.expenses.svcledger.entity.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

  /**
   * Find account by account code
   */
  Optional<Account> findByAccountCode(String accountCode);

  /**
   * Find accounts by owner
   */
  List<Account> findByOwnerTypeAndOwnerIdAndIsActiveTrueOrderByCreatedAt(
      Account.OwnerType ownerType, UUID ownerId);

  /**
   * Find account by owner and currency
   */
  Optional<Account> findByOwnerTypeAndOwnerIdAndCurrencyAndIsActiveTrue(
      Account.OwnerType ownerType, UUID ownerId, String currency);

  /**
   * Find all accounts for a user
   */
  @Query("SELECT a FROM Account a WHERE a.ownerType = 'USER' AND a.ownerId = :userId AND a.isActive = true ORDER BY a.currency")
  List<Account> findUserAccounts(@Param("userId") UUID userId);

  /**
   * Find all accounts for a group
   */
  @Query("SELECT a FROM Account a WHERE a.ownerType = 'GROUP' AND a.ownerId = :groupId AND a.isActive = true ORDER BY a.currency")
  List<Account> findGroupAccounts(@Param("groupId") UUID groupId);

  /**
   * Find accounts by currency
   */
  List<Account> findByCurrencyAndIsActiveTrueOrderByAccountName(String currency);

  /**
   * Find system accounts
   */
  @Query("SELECT a FROM Account a WHERE a.ownerType = 'SYSTEM' AND a.isActive = true ORDER BY a.currency")
  List<Account> findSystemAccounts();

  /**
   * Check if account exists for owner and currency
   */
  boolean existsByOwnerTypeAndOwnerIdAndCurrency(Account.OwnerType ownerType, UUID ownerId, String currency);

  /**
   * Find accounts with pagination and filtering
   */
  @Query("SELECT a FROM Account a WHERE " +
      "(:ownerType IS NULL OR a.ownerType = :ownerType) AND " +
      "(:ownerId IS NULL OR a.ownerId = :ownerId) AND " +
      "(:currency IS NULL OR a.currency = :currency) AND " +
      "(:isActive IS NULL OR a.isActive = :isActive) " +
      "ORDER BY a.createdAt DESC")
  Page<Account> findAccountsWithFilters(@Param("ownerType") Account.OwnerType ownerType,
      @Param("ownerId") UUID ownerId,
      @Param("currency") String currency,
      @Param("isActive") Boolean isActive,
      Pageable pageable);

  /**
   * Find accounts by name pattern
   */
  @Query("SELECT a FROM Account a WHERE " +
      "UPPER(a.accountName) LIKE UPPER(CONCAT('%', :namePattern, '%')) AND " +
      "a.isActive = true " +
      "ORDER BY a.accountName")
  List<Account> findByAccountNameContaining(@Param("namePattern") String namePattern);

  /**
   * Count accounts by owner type
   */
  long countByOwnerTypeAndIsActiveTrue(Account.OwnerType ownerType);

  /**
   * Find accounts created by specific user
   */
  @Query("SELECT a FROM Account a WHERE a.ownerId = :userId AND a.ownerType = 'USER' ORDER BY a.createdAt DESC")
  List<Account> findAccountsCreatedByUser(@Param("userId") UUID userId);

  /**
   * Find accounts that need balance reconciliation
   */
  @Query("SELECT a FROM Account a WHERE a.id NOT IN (SELECT ab.account.id FROM AccountBalance ab) AND a.isActive = true")
  List<Account> findAccountsWithoutBalance();

  /**
   * Get account statistics
   */
  @Query("SELECT " +
      "a.ownerType as ownerType, " +
      "a.currency as currency, " +
      "COUNT(a) as accountCount " +
      "FROM Account a WHERE a.isActive = true " +
      "GROUP BY a.ownerType, a.currency " +
      "ORDER BY a.ownerType, a.currency")
  List<Object[]> getAccountStatistics();
}
