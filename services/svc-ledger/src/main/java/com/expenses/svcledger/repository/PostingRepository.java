package com.expenses.svcledger.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.expenses.svcledger.entity.Posting;

@Repository
public interface PostingRepository extends JpaRepository<Posting, UUID> {
  List<Posting> findByJournalEntryId(UUID journalEntryId);
}
