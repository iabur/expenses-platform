package com.expenses.svcledger.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
import com.expenses.svcledger.repository.PostingRepository;
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
  private final PostingRepository postingRepository;
  private final AccountService accountService;

  @KafkaListener(topics = ExpenseEvent.TOPIC, groupId = "ledger-service")
  public void handleExpenseEvents(@Header(KafkaHeaders.RECEIVED_KEY) String key, DomainEvent event,
      Acknowledgment acknowledgment) {
    try {
      if (event == null) {
        log.warn("Received null event with key: {}", key);
        acknowledgment.acknowledge();
        return;
      }
      log.info("Processing expense event: {} (class={}) with key: {}", event.getEventName(), event.getClass().getName(),
          key);

      processEvent(event);
      acknowledgment.acknowledge();
    } catch (Exception e) {
      log.error("Error processing expense event (key={}, type={}): {}", key,
          event != null ? event.getClass().getName() : "null", e.getMessage(), e);
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
        case "EXPENSE_SPLITS_CALCULATED" ->
          handleExpenseSplitsCalculated((ExpenseEvent.ExpenseSplitsCalculated) expenseEvent);
        default -> log.debug("Unhandled expense event: {}", expenseEvent.getEventName());
      }
    }
  }

  /**
   * Handle expense splits calculated - create journal entries and postings with
   * actual calculated amounts
   */
  @Transactional
  private void handleExpenseSplitsCalculated(ExpenseEvent.ExpenseSplitsCalculated event) {
    log.info("Processing expense splits calculated event for expense: {}", event.getAggregateId());

    try {
      UUID expenseId = event.getAggregateId();
      UUID groupId = event.getGroupId();
      String currency = event.getCurrency();
      Long totalAmountCents = event.getTotalAmountCents();

      // Ensure system account exists
      Account systemAccount = getOrCreateSystemAccount(currency, "EXPENSES");

      // Create journal entry for the expense
      JournalEntry journalEntry = JournalEntry.builder()
          .referenceType(JournalEntry.ReferenceType.EXPENSE)
          .referenceId(expenseId)
          .groupId(groupId)
          .description("Expense splits calculated")
          .valueDate(LocalDate.now())
          .createdBy(event.getCausedBy())
          .build();

      List<Posting> postings = new ArrayList<>();

      // Create postings for each split
      if (event.getSplits() != null && !event.getSplits().isEmpty()) {
        for (ExpenseEvent.SplitInfo split : event.getSplits()) {
          UUID userId = split.getUserId();
          Long splitAmountCents = split.getAmountCents();

          if (splitAmountCents > 0) {
            // Ensure user account exists
            Account userAccount = getOrCreateUserAccount(userId, currency);

            // Create debit posting: Debit user account (user owes money)
            Posting debitPosting = Posting.builder()
                .journalEntry(journalEntry)
                .debitAccount(userAccount)
                .amountCents(splitAmountCents)
                .currency(currency)
                .description("Expense share for " + userId)
                .build();

            // Create credit posting: Credit system account (system receives money)
            Posting creditPosting = Posting.builder()
                .journalEntry(journalEntry)
                .creditAccount(systemAccount)
                .amountCents(splitAmountCents)
                .currency(currency)
                .description("Expense share for " + userId)
                .build();

            postings.add(debitPosting);
            postings.add(creditPosting);
          }
        }
      }

      // Add the payment posting (Debit system account, Credit paidBy account)
      if (totalAmountCents > 0) {
        UUID paidBy = event.getPaidBy();
        if (paidBy == null) {
          log.warn(
              "No paidBy information in expense splits calculated event for expense {}, using causedBy as fallback",
              expenseId);
          paidBy = event.getCausedBy();
        }

        Account paidByAccount = getOrCreateUserAccount(paidBy, currency);

        // Create debit posting: Debit system account (system paid the expense)
        Posting debitPaymentPosting = Posting.builder()
            .journalEntry(journalEntry)
            .debitAccount(systemAccount)
            .amountCents(totalAmountCents)
            .currency(currency)
            .description("Expense payment by " + paidBy)
            .build();

        // Create credit posting: Credit paidBy account (paidBy user should be
        // reimbursed)
        Posting creditPaymentPosting = Posting.builder()
            .journalEntry(journalEntry)
            .creditAccount(paidByAccount)
            .amountCents(totalAmountCents)
            .currency(currency)
            .description("Expense payment by " + paidBy)
            .build();

        postings.add(debitPaymentPosting);
        postings.add(creditPaymentPosting);
      }

      // Save journal entry first
      JournalEntry savedJournalEntry = journalEntryRepository.save(journalEntry);

      // Save postings individually to avoid cascade issues
      for (Posting posting : postings) {
        posting.setJournalEntry(savedJournalEntry);
        postingRepository.save(posting);
      }

      // Validate journal entry balance after all postings are saved
      validateJournalEntryBalance(savedJournalEntry.getId());

      // Update account balances
      updateAccountBalances(postings);

      log.info("Successfully created journal entry {} with {} postings for expense {} splits",
          journalEntry.getId(), postings.size(), expenseId);

    } catch (Exception e) {
      log.error("Failed to process expense splits calculation for expense {}: {}",
          event.getAggregateId(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Handle expense created - just log that it was processed
   * Actual journal entries will be created when EXPENSE_SPLITS_CALCULATED event
   * is received
   */
  private void handleExpenseCreated(ExpenseEvent.ExpenseCreated event) {
    log.info("Processing expense created event for expense: {} - waiting for splits calculation",
        event.getAggregateId());
    // No action needed here - journal entries will be created when splits are
    // calculated
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
  private Long calculateParticipantAmount(ExpenseEvent.ParticipantInfo participant, Long totalAmountCents,
      int participantCount) {
    // This is a simplified calculation - in production you might want to
    // get the actual calculated amounts from the Split Engine
    String ruleType = participant.getSplitRuleType();
    BigDecimal ruleValue = participant.getSplitRuleValue();

    if ("EQUAL".equals(ruleType)) {
      // For equal splits, divide by the actual number of participants
      return totalAmountCents / participantCount;
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
      if (posting.getDebitAccount() != null) {
        AccountBalance debitBalance = accountBalanceRepository
            .findByAccount(posting.getDebitAccount())
            .orElseGet(() -> {
              AccountBalance balance = new AccountBalance();
              balance.setAccount(posting.getDebitAccount());
              balance.setBalanceCents(0L);
              balance.setLastUpdated(ZonedDateTime.now());
              balance.setVersion(0L);
              return balance;
            });

        debitBalance.addToBalance(posting.getAmountCents());
        accountBalanceRepository.save(debitBalance);
      }

      // Update credit account balance
      if (posting.getCreditAccount() != null) {
        AccountBalance creditBalance = accountBalanceRepository
            .findByAccount(posting.getCreditAccount())
            .orElseGet(() -> {
              AccountBalance balance = new AccountBalance();
              balance.setAccount(posting.getCreditAccount());
              balance.setBalanceCents(0L);
              balance.setLastUpdated(ZonedDateTime.now());
              balance.setVersion(0L);
              return balance;
            });

        creditBalance.subtractFromBalance(posting.getAmountCents());
        accountBalanceRepository.save(creditBalance);
      }
    }
  }

  /**
   * Validate that a journal entry is balanced (total debits = total credits)
   */
  private void validateJournalEntryBalance(UUID journalEntryId) {
    // Get all postings for this journal entry
    List<Posting> postings = postingRepository.findByJournalEntryId(journalEntryId);

    long totalDebits = 0;
    long totalCredits = 0;

    for (Posting posting : postings) {
      if (posting.getDebitAccount() != null) {
        totalDebits += posting.getAmountCents();
      }
      if (posting.getCreditAccount() != null) {
        totalCredits += posting.getAmountCents();
      }
    }

    if (totalDebits != totalCredits) {
      throw new IllegalStateException(
          String.format("Journal entry %s is not balanced: debits (%d) != credits (%d)",
              journalEntryId, totalDebits, totalCredits));
    }

    log.debug("Journal entry {} is balanced: debits={}, credits={}", journalEntryId, totalDebits, totalCredits);
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof ExpenseEvent;
  }
}
