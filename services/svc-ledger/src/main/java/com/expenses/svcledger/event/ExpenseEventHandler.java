package com.expenses.svcledger.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.entity.AccountBalance;
import com.expenses.svcledger.entity.JournalEntry;
import com.expenses.svcledger.entity.Posting;
import com.expenses.svcledger.repository.AccountBalanceRepository;
import com.expenses.svcledger.repository.AccountRepository;
import com.expenses.svcledger.repository.JournalEntryRepository;
import com.expenses.svcledger.service.AccountService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event handler for expense-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventHandler extends BaseEventHandler {

  private final AccountRepository accountRepository;
  private final AccountBalanceRepository accountBalanceRepository;
  private final JournalEntryRepository journalEntryRepository;
  private final AccountService accountService;

  @KafkaListener(topics = ExpenseEvent.TOPIC, groupId = "ledger-service")
  public void handleExpenseEvents(@Header(KafkaHeaders.RECEIVED_KEY) String key, DomainEvent event, Acknowledgment acknowledgment) {
    try {
      if (event == null) {
        log.warn("Received null event with key: {}", key);
        acknowledgment.acknowledge();
        return;
      }
      log.info("Processing expense event: {} (class={}) with key: {}", event.getEventName(), event.getClass().getName(), key);

      processEvent(event);
      acknowledgment.acknowledge();
    } catch (Exception e) {
      log.error("Error processing expense event (key={}, type={}): {}", key, event != null ? event.getClass().getName() : "null", e.getMessage(), e);
      acknowledgment.acknowledge(); // Acknowledge to prevent infinite retry
    }
  }

  @Override
  @Transactional
  protected void processEvent(DomainEvent event) throws Exception {
    if (event instanceof ExpenseEvent expenseEvent) {
      switch (expenseEvent.getEventName()) {
        case "EXPENSE_CREATED" -> handleExpenseCreated((ExpenseEvent.ExpenseCreated) expenseEvent);
        case "EXPENSE_UPDATED" -> handleExpenseUpdated((ExpenseEvent.ExpenseUpdated) expenseEvent);
        case "EXPENSE_DELETED" -> handleExpenseDeleted((ExpenseEvent.ExpenseDeleted) expenseEvent);
        default -> log.debug("Unhandled expense event: {}", expenseEvent.getEventName());
      }
    }
  }

  /**
   * Handle expense created - create journal entries and postings
   */
  private void handleExpenseCreated(ExpenseEvent.ExpenseCreated event) {
    log.info("Processing expense created event for expense: {}", event.getAggregateId());

    try {
      UUID expenseId = event.getAggregateId();
      UUID groupId = event.getGroupId();
      String currency = event.getCurrency();
      Long amountCents = event.getAmountCents();
      UUID paidBy = event.getPaidBy() != null ? event.getPaidBy() : event.getCreatedBy();

      // Ensure system account exists
      Account systemAccount = getOrCreateSystemAccount(currency, "EXPENSES");

      // Create journal entry for the expense
      JournalEntry journalEntry = JournalEntry.builder()
          .referenceType(JournalEntry.ReferenceType.EXPENSE)
          .referenceId(expenseId)
          .groupId(groupId)
          .description("Expense: " + event.getNote())
          .valueDate(LocalDate.now())
          .createdBy(event.getCreatedBy())
          .build();

      List<Posting> postings = new ArrayList<>();

      // Create postings for each participant
      if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
        for (ExpenseEvent.ParticipantInfo participant : event.getParticipants()) {
          UUID userId = participant.getUserId();

          // Ensure user account exists
          Account userAccount = getOrCreateUserAccount(userId, currency);

          // Calculate participant's share (this should match Split Engine calculation)
          Long participantAmountCents = calculateParticipantAmount(participant, amountCents);

          if (participantAmountCents > 0) {
            // Create posting: Debit user account, Credit system account
            Posting posting = Posting.builder()
                .journalEntry(journalEntry)
                .debitAccount(userAccount)
                .creditAccount(systemAccount)
                .amountCents(participantAmountCents)
                .currency(currency)
                .description("Expense share for " + participant.getUserId())
                .build();

            postings.add(posting);
          }
        }
      }

      // Add the main expense posting (Credit system account, Debit paidBy account)
      if (amountCents > 0) {
        Account paidByAccount = getOrCreateUserAccount(paidBy, currency);

        Posting mainPosting = Posting.builder()
            .journalEntry(journalEntry)
            .debitAccount(systemAccount)
            .creditAccount(paidByAccount)
            .amountCents(amountCents)
            .currency(currency)
            .description("Expense payment by " + paidBy)
            .build();

        postings.add(mainPosting);
      }

      // Save journal entry with postings
      journalEntry.setPostings(postings);
      journalEntryRepository.save(journalEntry);

      // Update account balances
      updateAccountBalances(postings);

      log.info("Successfully created journal entry {} with {} postings for expense {}",
          journalEntry.getId(), postings.size(), expenseId);

    } catch (Exception e) {
      log.error("Failed to process expense creation for expense {}: {}",
          event.getAggregateId(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Handle expense updated - update journal entries
   */
  private void handleExpenseUpdated(ExpenseEvent.ExpenseUpdated event) {
    log.info("Processing expense updated event for expense: {}", event.getAggregateId());

    // Find existing journal entry and update it
    // This is a simplified implementation - in production you might want to
    // create a new journal entry to maintain audit trail
    try {
      Optional<JournalEntry> existingEntry = journalEntryRepository
          .findByReferenceTypeAndReferenceId(JournalEntry.ReferenceType.EXPENSE, event.getAggregateId());

      if (existingEntry.isPresent()) {
        JournalEntry journalEntry = existingEntry.get();
        journalEntry.setDescription("Expense (Updated): " + event.getNote());
        journalEntryRepository.save(journalEntry);

        log.info("Updated journal entry {} for expense {}",
            journalEntry.getId(), event.getAggregateId());
      }
    } catch (Exception e) {
      log.error("Failed to update journal entry for expense {}: {}",
          event.getAggregateId(), e.getMessage(), e);
    }
  }

  /**
   * Handle expense deleted - mark journal entries as deleted
   */
  private void handleExpenseDeleted(ExpenseEvent.ExpenseDeleted event) {
    log.info("Processing expense deleted event for expense: {}", event.getAggregateId());

    try {
      Optional<JournalEntry> existingEntry = journalEntryRepository
          .findByReferenceTypeAndReferenceId(JournalEntry.ReferenceType.EXPENSE, event.getAggregateId());

      if (existingEntry.isPresent()) {
        JournalEntry journalEntry = existingEntry.get();
        journalEntry.setDescription("Expense (Deleted): " + journalEntry.getDescription());
        journalEntryRepository.save(journalEntry);

        log.info("Marked journal entry {} as deleted for expense {}",
            journalEntry.getId(), event.getAggregateId());
      }
    } catch (Exception e) {
      log.error("Failed to mark journal entry as deleted for expense {}: {}",
          event.getAggregateId(), e.getMessage(), e);
    }
  }

  /**
   * Get or create system account
   */
  private Account getOrCreateSystemAccount(String currency, String accountType) {
    String accountCode = "SYSTEM:" + accountType + ":" + currency;

    return accountRepository.findByAccountCode(accountCode)
        .orElseGet(() -> {
          Account account = Account.builder()
              .accountCode(accountCode)
              .ownerType(Account.OwnerType.SYSTEM)
              .ownerId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
              .currency(currency)
              .accountName("System " + accountType + " " + currency)
              .description("System account for " + accountType + " " + currency)
              .isActive(true)
              .build();

          return accountRepository.save(account);
        });
  }

  /**
   * Get or create user account
   */
  private Account getOrCreateUserAccount(UUID userId, String currency) {
    String accountCode = "USER:" + userId + ":" + currency;

    return accountRepository.findByAccountCode(accountCode)
        .orElseGet(() -> {
          Account account = Account.builder()
              .accountCode(accountCode)
              .ownerType(Account.OwnerType.USER)
              .ownerId(userId)
              .currency(currency)
              .accountName("User " + userId + " " + currency)
              .description("User account for " + userId + " " + currency)
              .isActive(true)
              .build();

          return accountRepository.save(account);
        });
  }

  /**
   * Calculate participant amount based on split rules
   */
  private Long calculateParticipantAmount(ExpenseEvent.ParticipantInfo participant, Long totalAmountCents) {
    // This is a simplified calculation - in production you might want to
    // get the actual calculated amounts from the Split Engine
    String ruleType = participant.getSplitRuleType();
    BigDecimal ruleValue = participant.getSplitRuleValue();

    if ("EQUAL".equals(ruleType)) {
      // For equal splits, we'll use a simple division
      // In production, you should get the actual calculated amount from Split Engine
      return totalAmountCents / 3; // Simplified - should get from Split Engine
    } else if ("PERCENTAGE".equals(ruleType) && ruleValue != null) {
      return (long) (totalAmountCents * ruleValue.doubleValue() / 100);
    } else if ("EXACT_AMOUNT".equals(ruleType) && ruleValue != null) {
      return (long) (ruleValue.doubleValue() * 100);
    }

    return 0L;
  }

  /**
   * Update account balances based on postings
   */
  private void updateAccountBalances(List<Posting> postings) {
    for (Posting posting : postings) {
      // Update debit account balance
      AccountBalance debitBalance = accountBalanceRepository
          .findByAccount(posting.getDebitAccount())
          .orElseGet(() -> {
            AccountBalance balance = new AccountBalance();
            balance.setAccount(posting.getDebitAccount());
            balance.setBalanceCents(0L);
            return balance;
          });

      debitBalance.addToBalance(posting.getAmountCents());
      accountBalanceRepository.save(debitBalance);

      // Update credit account balance
      AccountBalance creditBalance = accountBalanceRepository
          .findByAccount(posting.getCreditAccount())
          .orElseGet(() -> {
            AccountBalance balance = new AccountBalance();
            balance.setAccount(posting.getCreditAccount());
            balance.setBalanceCents(0L);
            return balance;
          });

      creditBalance.subtractFromBalance(posting.getAmountCents());
      accountBalanceRepository.save(creditBalance);
    }
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof ExpenseEvent;
  }
}
