package com.expenses.svcsettle.repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcsettle.entity.SettlementPayment;

@Repository
public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, UUID> {

  /**
   * Find payments by proposal ID
   */
  List<SettlementPayment> findByProposalIdOrderByCreatedAtDesc(UUID proposalId);

  /**
   * Find payments where user is payer
   */
  Page<SettlementPayment> findByPayerIdOrderByCreatedAtDesc(UUID payerId, Pageable pageable);

  /**
   * Find payments where user is payee
   */
  Page<SettlementPayment> findByPayeeIdOrderByCreatedAtDesc(UUID payeeId, Pageable pageable);

  /**
   * Find payments involving a user (as payer or payee)
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.payerId = :userId OR p.payeeId = :userId " +
      "ORDER BY p.createdAt DESC")
  Page<SettlementPayment> findByUserInvolvement(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Find payments by status
   */
  Page<SettlementPayment> findByStatusOrderByCreatedAtDesc(SettlementPayment.PaymentStatus status, Pageable pageable);

  /**
   * Find pending payments for a user
   */
  @Query("SELECT p FROM SettlementPayment p WHERE " +
      "(p.payerId = :userId OR p.payeeId = :userId) " +
      "AND p.status IN ('PENDING', 'CONFIRMED') " +
      "ORDER BY p.dueDate ASC NULLS LAST, p.createdAt DESC")
  Page<SettlementPayment> findPendingPaymentsForUser(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Find overdue payments
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.dueDate < :now " +
      "AND p.status IN ('PENDING', 'CONFIRMED') " +
      "ORDER BY p.dueDate ASC")
  List<SettlementPayment> findOverduePayments(@Param("now") ZonedDateTime now);

  /**
   * Find overdue payments for a specific user
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.dueDate < :now " +
      "AND (p.payerId = :userId OR p.payeeId = :userId) " +
      "AND p.status IN ('PENDING', 'CONFIRMED') " +
      "ORDER BY p.dueDate ASC")
  List<SettlementPayment> findOverduePaymentsForUser(@Param("userId") UUID userId, @Param("now") ZonedDateTime now);

  /**
   * Find payments that need confirmation from payer
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.payerId = :userId " +
      "AND p.status = 'PENDING' " +
      "ORDER BY p.dueDate ASC NULLS LAST, p.createdAt DESC")
  Page<SettlementPayment> findPaymentsNeedingPayerConfirmation(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Find payments that need confirmation from payee
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.payeeId = :userId " +
      "AND p.status = 'CONFIRMED' " +
      "ORDER BY p.createdAt DESC")
  Page<SettlementPayment> findPaymentsNeedingPayeeConfirmation(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Find disputed payments
   */
  @Query("SELECT DISTINCT p FROM SettlementPayment p " +
      "JOIN p.confirmations c " +
      "WHERE c.confirmationType = 'DISPUTED' " +
      "AND p.status != 'CANCELLED' " +
      "ORDER BY c.createdAt DESC")
  Page<SettlementPayment> findDisputedPayments(Pageable pageable);

  /**
   * Find disputed payments for a user
   */
  @Query("SELECT DISTINCT p FROM SettlementPayment p " +
      "JOIN p.confirmations c " +
      "WHERE c.confirmationType = 'DISPUTED' " +
      "AND (p.payerId = :userId OR p.payeeId = :userId) " +
      "AND p.status != 'CANCELLED' " +
      "ORDER BY c.createdAt DESC")
  Page<SettlementPayment> findDisputedPaymentsForUser(@Param("userId") UUID userId, Pageable pageable);

  /**
   * Get payment statistics for a user
   */
  @Query("SELECT " +
      "COUNT(p), " +
      "COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END), " +
      "COUNT(CASE WHEN p.status = 'PENDING' THEN 1 END), " +
      "COUNT(CASE WHEN p.status = 'CONFIRMED' THEN 1 END), " +
      "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' AND p.payerId = :userId THEN p.amountCents ELSE 0 END), 0), " +
      "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' AND p.payeeId = :userId THEN p.amountCents ELSE 0 END), 0) " +
      "FROM SettlementPayment p WHERE p.payerId = :userId OR p.payeeId = :userId")
  Object[] getUserPaymentStatistics(@Param("userId") UUID userId);

  /**
   * Find payments between two users
   */
  @Query("SELECT p FROM SettlementPayment p WHERE " +
      "((p.payerId = :user1 AND p.payeeId = :user2) OR " +
      "(p.payerId = :user2 AND p.payeeId = :user1)) " +
      "ORDER BY p.createdAt DESC")
  Page<SettlementPayment> findPaymentsBetweenUsers(
      @Param("user1") UUID user1, @Param("user2") UUID user2, Pageable pageable);

  /**
   * Check if user has permission to access payment
   */
  @Query("SELECT COUNT(p) > 0 FROM SettlementPayment p WHERE p.id = :paymentId " +
      "AND (p.payerId = :userId OR p.payeeId = :userId)")
  boolean hasUserAccess(@Param("paymentId") UUID paymentId, @Param("userId") UUID userId);

  /**
   * Find payments by group (through proposal)
   */
  @Query("SELECT p FROM SettlementPayment p WHERE p.proposal.groupId = :groupId " +
      "ORDER BY p.createdAt DESC")
  Page<SettlementPayment> findByGroupId(@Param("groupId") UUID groupId, Pageable pageable);

  /**
   * Get group payment statistics
   */
  @Query("SELECT " +
      "COUNT(p), " +
      "COUNT(CASE WHEN p.status = 'COMPLETED' THEN 1 END), " +
      "COALESCE(SUM(CASE WHEN p.status = 'COMPLETED' THEN p.amountCents ELSE 0 END), 0) " +
      "FROM SettlementPayment p WHERE p.proposal.groupId = :groupId")
  Object[] getGroupPaymentStatistics(@Param("groupId") UUID groupId);

  /**
   * Find payments due soon (within specified days)
   */
  @Query("SELECT p FROM SettlementPayment p WHERE " +
      "p.dueDate BETWEEN :now AND :futureDate " +
      "AND p.status IN ('PENDING', 'CONFIRMED') " +
      "ORDER BY p.dueDate ASC")
  List<SettlementPayment> findPaymentsDueSoon(
      @Param("now") ZonedDateTime now, @Param("futureDate") ZonedDateTime futureDate);

  /**
   * Find completed payments in date range
   */
  @Query("SELECT p FROM SettlementPayment p WHERE " +
      "p.status = 'COMPLETED' " +
      "AND p.completedAt BETWEEN :startDate AND :endDate " +
      "ORDER BY p.completedAt DESC")
  Page<SettlementPayment> findCompletedPaymentsInDateRange(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      Pageable pageable);
}
