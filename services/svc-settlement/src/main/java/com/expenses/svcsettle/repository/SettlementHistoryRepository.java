package com.expenses.svcsettle.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.expenses.svcsettle.entity.SettlementHistory;

@Repository
public interface SettlementHistoryRepository extends JpaRepository<SettlementHistory, UUID> {

  /**
   * Find settlement history for a group
   */
  Page<SettlementHistory> findByGroupIdOrderByCreatedAtDesc(UUID groupId, Pageable pageable);

  /**
   * Find settlement history for a group with status filter
   */
  @Query("SELECT sh FROM SettlementHistory sh WHERE sh.groupId = :groupId " +
      "AND (:status IS NULL OR sh.status = :status) " +
      "ORDER BY sh.createdAt DESC")
  Page<SettlementHistory> findByGroupIdAndStatusOrderByCreatedAtDesc(
      @Param("groupId") UUID groupId, 
      @Param("status") String status, 
      Pageable pageable);

  /**
   * Find settlement history for a specific proposal
   */
  Page<SettlementHistory> findByProposalIdOrderByCreatedAtDesc(UUID proposalId, Pageable pageable);

  /**
   * Find settlement history for a specific payment
   */
  Page<SettlementHistory> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId, Pageable pageable);

  /**
   * Find settlement history for a user in a group
   */
  @Query("SELECT sh FROM SettlementHistory sh WHERE sh.groupId = :groupId " +
      "AND sh.userId = :userId " +
      "ORDER BY sh.createdAt DESC")
  Page<SettlementHistory> findByGroupIdAndUserIdOrderByCreatedAtDesc(
      @Param("groupId") UUID groupId, 
      @Param("userId") UUID userId, 
      Pageable pageable);

  /**
   * Find recent settlement activities for a group
   */
  @Query("SELECT sh FROM SettlementHistory sh WHERE sh.groupId = :groupId " +
      "AND sh.createdAt >= :since " +
      "ORDER BY sh.createdAt DESC")
  Page<SettlementHistory> findRecentByGroupId(
      @Param("groupId") UUID groupId, 
      @Param("since") java.time.ZonedDateTime since, 
      Pageable pageable);
}
