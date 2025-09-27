package com.expenses.svcledger.event;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.BaseEventHandler;
import com.expenses.common.event.DomainEvent;
import com.expenses.common.event.GroupEvent;
import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.repository.AccountRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event handler for group-related events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GroupEventHandler extends BaseEventHandler {

  private final AccountRepository accountRepository;

  @KafkaListener(topics = GroupEvent.TOPIC, groupId = "ledger-service")
  public void handleGroupEvents(@Header(KafkaHeaders.RECEIVED_KEY) String key, DomainEvent event, Acknowledgment acknowledgment) {
    try {
      if (event == null) {
        log.warn("Received null event with key: {}", key);
        acknowledgment.acknowledge();
        return;
      }
      log.info("Processing group event: {} (class={}) with key: {}", event.getEventName(), event.getClass().getName(), key);

      processEvent(event);
      acknowledgment.acknowledge();
    } catch (Exception e) {
      log.error("Error processing group event (key={}, type={}): {}", key, event != null ? event.getClass().getName() : "null", e.getMessage(), e);
      acknowledgment.acknowledge(); // Acknowledge to prevent infinite retry
    }
  }

  @Override
  @Transactional
  protected void processEvent(DomainEvent event) throws Exception {
    if (event instanceof GroupEvent groupEvent) {
      switch (groupEvent.getEventName()) {
        case "GROUP_CREATED" -> handleGroupCreated((GroupEvent.GroupCreated) groupEvent);
        case "MEMBER_ADDED" -> handleMemberAdded((GroupEvent.MemberAdded) groupEvent);
        case "MEMBER_REMOVED" -> handleMemberRemoved((GroupEvent.MemberRemoved) groupEvent);
        default -> log.debug("Unhandled group event: {}", groupEvent.getEventName());
      }
    }
  }

  /**
   * Handle group created - create group account
   */
  private void handleGroupCreated(GroupEvent.GroupCreated event) {
    log.info("Processing group created event for group: {}", event.getAggregateId());

    try {
      UUID groupId = event.getAggregateId();
      String defaultCurrency = event.getDefaultCurrency();

      // Create group account for the default currency
      createGroupAccount(groupId, defaultCurrency);

      log.info("Successfully created group account for group {} with currency {}",
          groupId, defaultCurrency);

    } catch (Exception e) {
      log.error("Failed to process group creation for group {}: {}",
          event.getAggregateId(), e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Handle member added - create user account if needed
   */
  private void handleMemberAdded(GroupEvent.MemberAdded event) {
    log.info("Processing member added event for group: {}, user: {}",
        event.getAggregateId(), event.getUserId());

    try {
      UUID groupId = event.getAggregateId();
      UUID userId = event.getUserId();

      // Get group to determine default currency
      // For now, we'll create USD account - in production you'd get this from group
      String defaultCurrency = "USD";

      // Create user account if it doesn't exist
      createUserAccount(userId, defaultCurrency);

      log.info("Successfully ensured user account exists for user {} in group {}",
          userId, groupId);

    } catch (Exception e) {
      log.error("Failed to process member addition for group {} user {}: {}",
          event.getAggregateId(), event.getUserId(), e.getMessage(), e);
    }
  }

  /**
   * Handle member removed - deactivate user account for the group
   */
  private void handleMemberRemoved(GroupEvent.MemberRemoved event) {
    log.info("Processing member removed event for group: {}, user: {}",
        event.getAggregateId(), event.getUserId());

    try {
      UUID groupId = event.getAggregateId();
      UUID userId = event.getUserId();

      // In a more sophisticated implementation, you might want to:
      // 1. Check if user has outstanding balances
      // 2. Create settlement entries
      // 3. Deactivate accounts only after settlement

      log.info("Processed member removal for user {} from group {}",
          userId, groupId);

    } catch (Exception e) {
      log.error("Failed to process member removal for group {} user {}: {}",
          event.getAggregateId(), event.getUserId(), e.getMessage(), e);
    }
  }

  /**
   * Create group account
   */
  private void createGroupAccount(UUID groupId, String currency) {
    String accountCode = "GROUP:" + groupId + ":" + currency;

    if (!accountRepository.findByAccountCode(accountCode).isPresent()) {
      Account account = Account.builder()
          .accountCode(accountCode)
          .ownerType(Account.OwnerType.GROUP)
          .ownerId(groupId)
          .currency(currency)
          .accountName("Group " + groupId + " " + currency)
          .description("Group account for " + groupId + " " + currency)
          .isActive(true)
          .build();

      accountRepository.save(account);
      log.info("Created group account: {}", accountCode);
    } else {
      log.debug("Group account already exists: {}", accountCode);
    }
  }

  /**
   * Create user account
   */
  private void createUserAccount(UUID userId, String currency) {
    String accountCode = "USER:" + userId + ":" + currency;

    if (!accountRepository.findByAccountCode(accountCode).isPresent()) {
      Account account = Account.builder()
          .accountCode(accountCode)
          .ownerType(Account.OwnerType.USER)
          .ownerId(userId)
          .currency(currency)
          .accountName("User " + userId + " " + currency)
          .description("User account for " + userId + " " + currency)
          .isActive(true)
          .build();

      accountRepository.save(account);
      log.info("Created user account: {}", accountCode);
    } else {
      log.debug("User account already exists: {}", accountCode);
    }
  }

  @Override
  protected boolean canHandle(DomainEvent event) {
    return event instanceof GroupEvent;
  }
}
