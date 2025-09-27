package com.expenses.svcsplitengine.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcsplitengine.dto.DebtSummary;
import com.expenses.svcsplitengine.dto.OptimizedDebtResult;
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

    // Calculate total expense amount (for logging)
    Long totalExpenseAmount = splits.stream()
        .mapToLong(ExpenseEvent.SplitInfo::getAmountCents)
        .sum();
    
    log.debug("Total expense amount: {} cents", totalExpenseAmount);

    // For each participant, update their balance
    // Each participant owes their split amount (debit)
    for (ExpenseEvent.SplitInfo split : splits) {
      updateUserBalance(groupId, split.getUserId(), currency,
          -split.getAmountCents(), expenseId);
    }

    log.info("Successfully updated balances for {} participants in group {}",
        splits.size(), groupId);
  }

  /**
   * Update group balances from expense creation (includes crediting payer)
   */
  public void updateBalancesFromExpense(UUID groupId, UUID expenseId, UUID paidBy,
      List<ExpenseEvent.SplitInfo> splits, String currency) {
    log.info("Updating balances for group {} from expense {} paid by {} with {} splits",
        groupId, expenseId, paidBy, splits.size());

    // Calculate total expense amount
    Long totalExpenseAmount = splits.stream()
        .mapToLong(ExpenseEvent.SplitInfo::getAmountCents)
        .sum();

    // Credit the payer for the full expense amount
    updateUserBalance(groupId, paidBy, currency, totalExpenseAmount, expenseId);

    // Debit each participant for their split amount
    for (ExpenseEvent.SplitInfo split : splits) {
      updateUserBalance(groupId, split.getUserId(), currency,
          -split.getAmountCents(), expenseId);
    }

    log.info("Successfully updated balances: credited {} cents to payer {}, debited {} participants",
        totalExpenseAmount, paidBy, splits.size());
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

  /**
   * Get user balances across all groups
   */
  @Transactional(readOnly = true)
  public Page<GroupBalance> getUserBalances(UUID userId, Pageable pageable) {
    return groupBalanceRepository.findByUserIdOrderByBalanceCentsDesc(userId, pageable);
  }

  /**
   * Get simplified debt relationships for a group
   */
  @Transactional(readOnly = true)
  public List<DebtSummary> getGroupDebts(UUID groupId) {
    List<GroupBalance> debtors = getDebtors(groupId);
    List<GroupBalance> creditors = getCreditors(groupId);

    List<DebtSummary> debts = new ArrayList<>();

    // Simple debt optimization: match debtors with creditors
    int debtorIndex = 0;
    int creditorIndex = 0;

    while (debtorIndex < debtors.size() && creditorIndex < creditors.size()) {
      GroupBalance debtor = debtors.get(debtorIndex);
      GroupBalance creditor = creditors.get(creditorIndex);

      // Calculate debt amount (minimum of what debtor owes and what creditor is owed)
      BigDecimal debtorAmount = debtor.getBalanceDecimal().abs();
      BigDecimal creditorAmount = creditor.getBalanceDecimal();
      BigDecimal debtAmount = debtorAmount.min(creditorAmount);

      if (debtAmount.compareTo(BigDecimal.ZERO) > 0) {
        debts.add(DebtSummary.builder()
            .debtorId(debtor.getUserId())
            .creditorId(creditor.getUserId())
            .amount(debtAmount)
            .currency(debtor.getCurrency())
            .description(String.format("Debt from %s to %s", debtor.getUserId(), creditor.getUserId()))
            .build());
      }

      // Update balances
      debtor.addToBalance(debtAmount.multiply(BigDecimal.valueOf(100)).longValue());
      creditor.addToBalance(-debtAmount.multiply(BigDecimal.valueOf(100)).longValue());

      // Move to next debtor/creditor if current one is settled
      if (debtor.getBalanceCents() >= 0) {
        debtorIndex++;
      }
      if (creditor.getBalanceCents() <= 0) {
        creditorIndex++;
      }
    }

    return debts;
  }

  /**
   * Optimize group debts to minimize number of transactions
   */
  @Transactional(readOnly = true)
  public OptimizedDebtResult optimizeGroupDebts(UUID groupId) {
    List<GroupBalance> debtors = getDebtors(groupId);
    List<GroupBalance> creditors = getCreditors(groupId);

    // Calculate original transaction count (naive approach)
    int originalCount = debtors.size() + creditors.size();

    // Get simplified debt relationships
    List<DebtSummary> debts = getGroupDebts(groupId);
    int optimizedCount = debts.size();

    // Calculate totals
    BigDecimal totalDebt = debtors.stream()
        .map(GroupBalance::getBalanceDecimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .abs();

    BigDecimal totalCredit = creditors.stream()
        .map(GroupBalance::getBalanceDecimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Calculate savings percentage
    BigDecimal savingsPercentage = originalCount > 0 
        ? BigDecimal.valueOf(100).subtract(
            BigDecimal.valueOf(optimizedCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(originalCount), 2, java.math.RoundingMode.HALF_UP))
        : BigDecimal.ZERO;

    // Convert debts to optimized transactions
    List<OptimizedDebtResult.OptimizedTransaction> transactions = debts.stream()
        .map(debt -> OptimizedDebtResult.OptimizedTransaction.builder()
            .payerId(debt.debtorId())
            .payeeId(debt.creditorId())
            .amount(debt.amount())
            .currency(debt.currency())
            .description(debt.description())
            .build())
        .toList();

    return OptimizedDebtResult.builder()
        .groupId(groupId)
        .currency(creditors.isEmpty() ? "USD" : creditors.get(0).getCurrency())
        .totalDebtAmount(totalDebt)
        .totalCreditAmount(totalCredit)
        .originalTransactionCount(originalCount)
        .optimizedTransactionCount(optimizedCount)
        .savingsPercentage(savingsPercentage)
        .transactions(transactions)
        .build();
  }
}
