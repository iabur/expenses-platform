package com.expenses.svcsplitengine.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcsplitengine.entity.GroupBalance;
import com.expenses.svcsplitengine.repository.GroupBalanceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BalanceUpdateService {

  private final GroupBalanceRepository groupBalanceRepository;

  /**
   * Update group balances based on calculated expense splits
   */
  public void updateBalancesFromSplits(UUID groupId, UUID expenseId,
      List<ExpenseEvent.SplitInfo> splits,
      String currency) {
    log.info("Updating balances for group {} from expense {} with {} splits",
        groupId, expenseId, splits.size());

    // Calculate total expense amount
    Long totalExpenseAmount = splits.stream()
        .mapToLong(ExpenseEvent.SplitInfo::getAmountCents)
        .sum();

    // For each participant, update their balance
    // Assumption: The person who paid the expense should be credited
    // and all participants should be debited for their share

    for (ExpenseEvent.SplitInfo split : splits) {
      // Each participant owes their split amount
      updateUserBalance(groupId, split.getUserId(), currency,
          -split.getAmountCents(), expenseId);
    }

    // Note: We need to know who paid the expense to credit them
    // This information should come from the expense event
    // For now, we'll handle the credit separately when we have payer info

    log.info("Successfully updated balances for {} participants in group {}",
        splits.size(), groupId);
  }

  /**
   * Update balance for a specific user in a group
   */
  public void updateUserBalance(UUID groupId, UUID userId, String currency,
      Long amountCents, UUID expenseId) {

    GroupBalance balance = groupBalanceRepository
        .findByGroupIdAndUserIdAndCurrency(groupId, userId, currency)
        .orElse(GroupBalance.builder()
            .groupId(groupId)
            .userId(userId)
            .currency(currency)
            .balanceCents(0L)
            .build());

    Long previousBalance = balance.getBalanceCents();
    balance.addToBalance(amountCents);
    balance.setLastExpenseId(expenseId);

    GroupBalance savedBalance = groupBalanceRepository.save(balance);

    log.debug("Updated balance for user {} in group {}: {} -> {} cents",
        userId, groupId, previousBalance, savedBalance.getBalanceCents());
  }

  /**
   * Credit the expense payer
   */
  public void creditExpensePayer(UUID groupId, UUID payerId, String currency,
      Long totalAmountCents, UUID expenseId) {
    log.info("Crediting expense payer {} for {} cents in group {}",
        payerId, totalAmountCents, groupId);

    updateUserBalance(groupId, payerId, currency, totalAmountCents, expenseId);
  }

  /**
   * Get group balances summary
   */
  @Transactional(readOnly = true)
  public List<GroupBalance> getGroupBalances(UUID groupId) {
    return groupBalanceRepository.findByGroupIdOrderByBalanceCentsDesc(groupId);
  }

  /**
   * Get group balances for specific currency
   */
  @Transactional(readOnly = true)
  public List<GroupBalance> getGroupBalances(UUID groupId, String currency) {
    return groupBalanceRepository.findByGroupIdAndCurrencyOrderByBalanceCentsDesc(
        groupId, currency);
  }

  /**
   * Get user's balance in a group
   */
  @Transactional(readOnly = true)
  public GroupBalance getUserBalance(UUID groupId, UUID userId, String currency) {
    return groupBalanceRepository
        .findByGroupIdAndUserIdAndCurrency(groupId, userId, currency)
        .orElse(GroupBalance.builder()
            .groupId(groupId)
            .userId(userId)
            .currency(currency)
            .balanceCents(0L)
            .build());
  }

  /**
   * Check if group is balanced (all balances sum to zero)
   */
  @Transactional(readOnly = true)
  public boolean isGroupBalanced(UUID groupId, String currency) {
    return groupBalanceRepository.isGroupBalanced(groupId, currency);
  }

  /**
   * Get total group debt amount
   */
  @Transactional(readOnly = true)
  public BigDecimal getTotalGroupDebt(UUID groupId, String currency) {
    Long debtCents = groupBalanceRepository.getTotalGroupDebt(groupId, currency);
    return BigDecimal.valueOf(debtCents).divide(BigDecimal.valueOf(100));
  }

  /**
   * Get users who owe money in the group
   */
  @Transactional(readOnly = true)
  public List<GroupBalance> getDebtors(UUID groupId) {
    return groupBalanceRepository.findDebtorsInGroup(groupId);
  }

  /**
   * Get users who are owed money in the group
   */
  @Transactional(readOnly = true)
  public List<GroupBalance> getCreditors(UUID groupId) {
    return groupBalanceRepository.findCreditorsInGroup(groupId);
  }
}
