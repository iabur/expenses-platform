package com.expenses.svcledger.event;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.SettlementEvent;
import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.entity.AccountBalance;
import com.expenses.svcledger.entity.JournalEntry;
import com.expenses.svcledger.entity.Posting;
import com.expenses.svcledger.repository.AccountBalanceRepository;
import com.expenses.svcledger.repository.AccountRepository;
import com.expenses.svcledger.repository.JournalEntryRepository;
import com.expenses.svcledger.repository.PostingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event handler for settlement events.
 * Records journal entries for settlement payments.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventHandler extends BaseEventHandler {

  private final JournalEntryRepository journalEntryRepository;
  private final PostingRepository postingRepository;
  private final AccountRepository accountRepository;
  private final AccountBalanceRepository accountBalanceRepository;

  @org.springframework.kafka.annotation.KafkaListener(topics = "settlement-events", groupId = "ledger-service")
  public void handleSettlementEvents(ConsumerRecord<String, DomainEvent> record, Acknowledgment acknowledgment) {
    DomainEvent event = record.value();
    String topic = record.topic();
    int partition = record.partition();
    long offset = record.offset();

    handleEvent(event, topic, partition, offset, record, acknowledgment);
  }

  @Override
  protected void processEvent(DomainEvent event) throws Exception {
    if (event instanceof SettlementEvent settlementEvent) {
      switch (settlementEvent.getEventName()) {
        case "PAYMENT_COMPLETED" -> handlePaymentCompleted((SettlementEvent.PaymentCompleted) settlementEvent);
        default -> log.debug("Unhandled settlement event: {}", settlementEvent.getEventName());
      }
    }
  }

  /**
   * Handle payment completed - create journal entry for the settlement payment
   * 
   * Accounting entries:
   * Debit: Payer's payable account (they paid off debt)
   * Credit: Payee's receivable account (they received what they were owed)
   */
  private void handlePaymentCompleted(SettlementEvent.PaymentCompleted event) {
    log.info("Processing payment completed event for payment: {} - {} cents from {} to {}",
        event.getPaymentId(), event.getAmountCents(), event.getPayerId(), event.getPayeeId());

    try {
      UUID paymentId = event.getPaymentId();
      UUID proposalId = event.getProposalId();
      UUID groupId = event.getGroupId();
      UUID payerId = event.getPayerId();
      UUID payeeId = event.getPayeeId();
      Long amountCents = event.getAmountCents();
      String currency = event.getCurrency();

      // Create journal entry
      // Use group ID as the "created by" since this is a system-generated entry
      JournalEntry journalEntry = JournalEntry.builder()
          .referenceType(JournalEntry.ReferenceType.SETTLEMENT)
          .referenceId(paymentId)
          .groupId(groupId)
          .description(String.format("Settlement payment: %s %s from user %s to user %s (Proposal: %s)",
              amountCents / 100.0, currency, payerId, payeeId, proposalId))
          .valueDate(LocalDate.now())
          .createdBy(payerId) // Use payer as the creator
          .build();

      // Create postings
      List<Posting> postings = new ArrayList<>();

      // Get or create accounts for payer and payee
      Account payerAccount = getOrCreateUserAccount(payerId, currency);
      Account payeeAccount = getOrCreateUserAccount(payeeId, currency);

      // Debit payer's account (they paid off debt, reducing their liability)
      Posting payerDebit = Posting.builder()
          .debitAccount(payerAccount)
          .amountCents(amountCents)
          .currency(currency)
          .description(String.format("Settlement payment to %s", payeeId))
          .build();
      postings.add(payerDebit);

      // Credit payee's account (they received money, reducing their receivable)
      Posting payeeCredit = Posting.builder()
          .creditAccount(payeeAccount)
          .amountCents(amountCents)
          .currency(currency)
          .description(String.format("Settlement payment from %s", payerId))
          .build();
      postings.add(payeeCredit);

      // Save journal entry first
      JournalEntry savedJournalEntry = journalEntryRepository.save(journalEntry);

      // Save postings
      for (Posting posting : postings) {
        posting.setJournalEntry(savedJournalEntry);
        postingRepository.save(posting);
      }

      // Update account balances
      updateAccountBalances(postings);

      log.info("Successfully created journal entry {} for settlement payment {} - {} {}: {} -> {}",
          savedJournalEntry.getId(), paymentId, amountCents / 100.0, currency, payerId, payeeId);

    } catch (Exception e) {
      log.error("Failed to process payment completed event for payment {}: {}",
          event.getPaymentId(), e.getMessage(), e);
      throw e; // Rethrow to trigger retry
    }
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
              .accountName(String.format("User %s - %s", userId, currency))
              .ownerType(Account.OwnerType.USER)
              .ownerId(userId)
              .currency(currency)
              .build();

          Account savedAccount = accountRepository.save(account);
          log.info("Created new account: {} for user {}", accountCode, userId);

          return savedAccount;
        });
  }

  /**
   * Update account balances based on postings
   */
  private void updateAccountBalances(List<Posting> postings) {
    for (Posting posting : postings) {
      // Update debit account balance
      if (posting.getDebitAccount() != null) {
        updateAccountBalance(posting.getDebitAccount(), posting.getAmountCents(), true);
      }

      // Update credit account balance
      if (posting.getCreditAccount() != null) {
        updateAccountBalance(posting.getCreditAccount(), posting.getAmountCents(), false);
      }
    }
  }

  /**
   * Update single account balance
   */
  private void updateAccountBalance(Account account, Long amountCents, boolean isDebit) {
    AccountBalance balance = accountBalanceRepository
        .findByAccountId(account.getId())
        .orElse(AccountBalance.builder()
            .account(account)
            .balanceCents(0L)
            .build());

    Long previousBalance = balance.getBalanceCents();

    // Debit increases balance, credit decreases balance (for asset accounts)
    // For liability/equity accounts, it's reversed
    if (isDebit) {
      balance.addToBalance(amountCents);
    } else {
      balance.addToBalance(-amountCents);
    }

    accountBalanceRepository.save(balance);

    log.debug("Updated account balance for {}: {} -> {} cents ({})",
        account.getAccountCode(), previousBalance, balance.getBalanceCents(),
        isDebit ? "debit" : "credit");
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof SettlementEvent;
  }
}
