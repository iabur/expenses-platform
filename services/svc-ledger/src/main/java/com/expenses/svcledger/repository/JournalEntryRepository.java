package com.expenses.svcledger.repository;

import com.expenses.svcledger.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

  /**
   * Find journal entry by reference
   */
  Optional<JournalEntry> findByReferenceTypeAndReferenceId(
      JournalEntry.ReferenceType referenceType, UUID referenceId);

  /**
   * Find journal entries for a group
   */
  Page<JournalEntry> findByGroupIdOrderByValueDateDescCreatedAtDesc(UUID groupId, Pageable pageable);

  /**
   * Find journal entries by date range
   */
  @Query("SELECT je FROM JournalEntry je WHERE " +
      "je.valueDate BETWEEN :startDate AND :endDate " +
      "ORDER BY je.valueDate DESC, je.createdAt DESC")
  List<JournalEntry> findByDateRange(@Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * Find journal entries for a group within date range
   */
  @Query("SELECT je FROM JournalEntry je WHERE " +
      "je.groupId = :groupId AND " +
      "je.valueDate BETWEEN :startDate AND :endDate " +
      "ORDER BY je.valueDate DESC, je.createdAt DESC")
  List<JournalEntry> findByGroupAndDateRange(@Param("groupId") UUID groupId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);

  /**
   * Find journal entries by reference type
   */
  List<JournalEntry> findByReferenceTypeOrderByCreatedAtDesc(JournalEntry.ReferenceType referenceType);

  /**
   * Find journal entries created by user
   */
  Page<JournalEntry> findByCreatedByOrderByCreatedAtDesc(UUID createdBy, Pageable pageable);

  /**
   * Find recent journal entries
   */
  @Query("SELECT je FROM JournalEntry je WHERE je.createdAt >= :since ORDER BY je.createdAt DESC")
  List<JournalEntry> findRecentEntries(@Param("since") java.time.ZonedDateTime since);

  /**
   * Find journal entries with filters
   */
  @Query("SELECT je FROM JournalEntry je WHERE " +
      "(:groupId IS NULL OR je.groupId = :groupId) AND " +
      "(:referenceType IS NULL OR je.referenceType = :referenceType) AND " +
      "(:createdBy IS NULL OR je.createdBy = :createdBy) AND " +
      "(:startDate IS NULL OR je.valueDate >= :startDate) AND " +
      "(:endDate IS NULL OR je.valueDate <= :endDate) " +
      "ORDER BY je.valueDate DESC, je.createdAt DESC")
  Page<JournalEntry> findEntriesWithFilters(@Param("groupId") UUID groupId,
      @Param("referenceType") JournalEntry.ReferenceType referenceType,
      @Param("createdBy") UUID createdBy,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate,
      Pageable pageable);

  /**
   * Count entries by reference type
   */
  long countByReferenceType(JournalEntry.ReferenceType referenceType);

  /**
   * Count entries for a group
   */
  long countByGroupId(UUID groupId);

  /**
   * Find entries that may be unbalanced
   */
  @Query("SELECT je FROM JournalEntry je WHERE " +
      "je.id NOT IN (" +
      "   SELECT p.journalEntry.id FROM Posting p " +
      "   GROUP BY p.journalEntry.id " +
      "   HAVING SUM(CASE WHEN p.debitAccount IS NOT NULL THEN p.amountCents ELSE 0 END) = " +
      "          SUM(CASE WHEN p.creditAccount IS NOT NULL THEN p.amountCents ELSE 0 END)" +
      ") " +
      "ORDER BY je.createdAt DESC")
  List<JournalEntry> findUnbalancedEntries();

  /**
   * Get journal entry statistics
   */
  @Query("SELECT " +
      "je.referenceType as referenceType, " +
      "COUNT(je) as entryCount, " +
      "MIN(je.valueDate) as earliestDate, " +
      "MAX(je.valueDate) as latestDate " +
      "FROM JournalEntry je " +
      "GROUP BY je.referenceType " +
      "ORDER BY je.referenceType")
  List<Object[]> getJournalEntryStatistics();

  /**
   * Find entries by description pattern
   */
  @Query("SELECT je FROM JournalEntry je WHERE " +
      "UPPER(je.description) LIKE UPPER(CONCAT('%', :pattern, '%')) " +
      "ORDER BY je.createdAt DESC")
  List<JournalEntry> findByDescriptionContaining(@Param("pattern") String pattern, Pageable pageable);
}
