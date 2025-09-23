package com.expenses.svcexpense.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.common.event.EventPublisher;
import com.expenses.common.event.ExpenseEvent;
import com.expenses.svcexpense.dto.ExpenseDto;
import com.expenses.svcexpense.entity.Expense;
import com.expenses.svcexpense.entity.ExpenseLineItem;
import com.expenses.svcexpense.entity.ExpenseParticipant;
import com.expenses.svcexpense.exception.ExpenseNotFoundException;
import com.expenses.svcexpense.exception.UnauthorizedExpenseAccessException;
import com.expenses.svcexpense.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

  private final ExpenseRepository expenseRepository;
  private final EventPublisher eventPublisher;

  /**
   * Create a new expense
   */
  public ExpenseDto.ExpenseResponse createExpense(ExpenseDto.CreateExpenseRequest request,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    log.info("Creating expense for group {} by user {}", request.groupId(), currentUserId);

    // Create expense entity
    Expense expense = new Expense();
    expense.setGroupId(request.groupId());
    expense.setCreatorId(currentUserId);
    expense.setCurrency(request.currency());
    expense.setAmountDecimal(request.amount());
    expense.setOccurredAt(request.occurredAt());
    expense.setNote(request.note());
    expense.setCategory(request.category());

    // Add participants
    for (var participantReq : request.participants()) {
      ExpenseParticipant participant = new ExpenseParticipant();
      participant.setUserId(participantReq.userId());
      participant.setRuleType(participantReq.ruleType());
      participant.setRuleValue(participantReq.ruleValue());
      expense.addParticipant(participant);
    }

    // Add line items if provided
    if (request.lineItems() != null) {
      for (var lineItemReq : request.lineItems()) {
        ExpenseLineItem lineItem = new ExpenseLineItem();
        lineItem.setDescription(lineItemReq.description());
        lineItem.setQuantity(lineItemReq.quantity());
        lineItem.setUnitPriceDecimal(lineItemReq.unitPrice());
        lineItem.setCategory(lineItemReq.category());
        expense.addLineItem(lineItem);
      }
    }

    Expense savedExpense = expenseRepository.save(expense);

    // Publish expense created event
    publishExpenseCreatedEvent(savedExpense, request.paidBy());

    log.info("Created expense {} for group {}", savedExpense.getId(), request.groupId());

    return ExpenseDto.ExpenseResponse.from(savedExpense);
  }

  /**
   * Get expense by ID
   */
  @Transactional(readOnly = true)
  public ExpenseDto.ExpenseResponse getExpense(UUID expenseId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Expense expense = expenseRepository.findByIdAndIsDeletedFalse(expenseId)
        .orElseThrow(() -> new ExpenseNotFoundException("Expense not found: " + expenseId));

    // Check if user has access to this expense (member of the group)
    if (!hasExpenseAccess(expense, currentUserId)) {
      throw new UnauthorizedExpenseAccessException("You don't have access to this expense");
    }

    return ExpenseDto.ExpenseResponse.from(expense);
  }

  /**
   * Update expense
   */
  public ExpenseDto.ExpenseResponse updateExpense(UUID expenseId,
      ExpenseDto.UpdateExpenseRequest request,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Expense expense = expenseRepository.findByIdAndIsDeletedFalse(expenseId)
        .orElseThrow(() -> new ExpenseNotFoundException("Expense not found: " + expenseId));

    // Check if user can modify this expense (creator or group admin)
    if (!canModifyExpense(expense, currentUserId)) {
      throw new UnauthorizedExpenseAccessException("You don't have permission to modify this expense");
    }

    // Update basic fields
    expense.setCurrency(request.currency());
    expense.setAmountDecimal(request.amount());
    expense.setOccurredAt(request.occurredAt());
    expense.setNote(request.note());
    expense.setCategory(request.category());

    // Update participants - clear and re-add
    expense.getParticipants().clear();
    for (var participantReq : request.participants()) {
      ExpenseParticipant participant = new ExpenseParticipant();
      participant.setUserId(participantReq.userId());
      participant.setRuleType(participantReq.ruleType());
      participant.setRuleValue(participantReq.ruleValue());
      expense.addParticipant(participant);
    }

    // Update line items - clear and re-add
    if (request.lineItems() != null) {
      expense.getLineItems().clear();
      for (var lineItemReq : request.lineItems()) {
        ExpenseLineItem lineItem = new ExpenseLineItem();
        lineItem.setDescription(lineItemReq.description());
        lineItem.setQuantity(lineItemReq.quantity());
        lineItem.setUnitPriceDecimal(lineItemReq.unitPrice());
        lineItem.setCategory(lineItemReq.category());
        expense.addLineItem(lineItem);
      }
    }

    Expense savedExpense = expenseRepository.save(expense);

    // Publish expense updated event
    publishExpenseUpdatedEvent(savedExpense);

    log.info("Updated expense {} by user {}", expenseId, currentUserId);

    return ExpenseDto.ExpenseResponse.from(savedExpense);
  }

  /**
   * Delete expense (soft delete)
   */
  public void deleteExpense(UUID expenseId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Expense expense = expenseRepository.findByIdAndIsDeletedFalse(expenseId)
        .orElseThrow(() -> new ExpenseNotFoundException("Expense not found: " + expenseId));

    // Check if user can delete this expense (creator or group admin)
    if (!canModifyExpense(expense, currentUserId)) {
      throw new UnauthorizedExpenseAccessException("You don't have permission to delete this expense");
    }

    expense.softDelete();
    expenseRepository.save(expense);

    // Publish expense deleted event
    ExpenseEvent.ExpenseDeleted event = new ExpenseEvent.ExpenseDeleted(
        expenseId, expense.getGroupId(), currentUserId);
    eventPublisher.publishEventAsync(event);

    log.info("Deleted expense {} by user {}", expenseId, currentUserId);
  }

  /**
   * Get expenses for a group
   */
  @Transactional(readOnly = true)
  public Page<ExpenseDto.ExpenseSummary> getGroupExpenses(UUID groupId,
      Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Verify user is member of the group

    return expenseRepository.findByGroupIdAndIsDeletedFalseOrderByCreatedAtDesc(groupId, pageable)
        .map(ExpenseDto.ExpenseSummary::from);
  }

  /**
   * Get expenses created by current user
   */
  @Transactional(readOnly = true)
  public Page<ExpenseDto.ExpenseSummary> getUserExpenses(Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    return expenseRepository.findByCreatorIdAndIsDeletedFalseOrderByCreatedAtDesc(currentUserId, pageable)
        .map(ExpenseDto.ExpenseSummary::from);
  }

  /**
   * Get expenses where user is a participant
   */
  @Transactional(readOnly = true)
  public Page<ExpenseDto.ExpenseSummary> getUserParticipatedExpenses(Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    return expenseRepository.findByParticipantsUserIdAndIsDeletedFalseOrderByCreatedAtDesc(currentUserId, pageable)
        .map(ExpenseDto.ExpenseSummary::from);
  }

  /**
   * Search expenses by category or note
   */
  @Transactional(readOnly = true)
  public Page<ExpenseDto.ExpenseSummary> searchExpenses(String query,
      UUID groupId,
      Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    if (groupId != null) {
      // TODO: Verify user is member of the group
      return expenseRepository.searchInGroup(query, groupId, pageable)
          .map(ExpenseDto.ExpenseSummary::from);
    } else {
      return expenseRepository.searchUserExpenses(query, currentUserId, pageable)
          .map(ExpenseDto.ExpenseSummary::from);
    }
  }

  /**
   * Get expense statistics for a group
   */
  @Transactional(readOnly = true)
  public ExpenseStatistics getGroupExpenseStatistics(UUID groupId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Verify user is member of the group

    Long totalCount = expenseRepository.countByGroupIdAndIsDeletedFalse(groupId);
    BigDecimal totalAmount = expenseRepository.sumAmountByGroupId(groupId);
    List<CategoryTotal> categoryTotals = expenseRepository.getCategoryTotals(groupId);

    return new ExpenseStatistics(totalCount, totalAmount, categoryTotals);
  }

  // Helper methods

  private UUID getCurrentUserId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return UUID.fromString(jwt.getSubject());
    }
    throw new IllegalArgumentException("Invalid authentication type");
  }

  private boolean hasExpenseAccess(Expense expense, UUID userId) {
    // User has access if they are the creator or a participant
    // TODO: Also check if user is member of the group
    return expense.getCreatorId().equals(userId) ||
        expense.getParticipants().stream()
            .anyMatch(p -> p.getUserId().equals(userId));
  }

  private boolean canModifyExpense(Expense expense, UUID userId) {
    // Only creator can modify for now
    // TODO: Also allow group admins to modify
    return expense.getCreatorId().equals(userId);
  }

  private void publishExpenseCreatedEvent(Expense expense, UUID paidByOverride) {
    List<ExpenseEvent.ParticipantInfo> participants = expense.getParticipants().stream()
        .map(p -> ExpenseEvent.ParticipantInfo.builder()
            .userId(p.getUserId())
            .splitRuleType(p.getRuleType().name())
            .splitRuleValue(p.getRuleValue())
            .build())
        .collect(java.util.stream.Collectors.toList());

    UUID paidBy = paidByOverride != null ? paidByOverride : expense.getCreatorId();

    ExpenseEvent.ExpenseCreated event = new ExpenseEvent.ExpenseCreated(
        expense.getId(),
        expense.getGroupId(),
        expense.getCurrency(),
        expense.getAmountCents(),
        expense.getOccurredAt(),
        expense.getNote(),
        expense.getCategory(),
        expense.getCreatorId(),
        paidBy,
        participants);

    eventPublisher.publishEventAsync(event);
  }

  private void publishExpenseUpdatedEvent(Expense expense) {
    ExpenseEvent.ExpenseUpdated event = new ExpenseEvent.ExpenseUpdated(
        expense.getId(),
        expense.getCurrency(),
        expense.getAmountCents(),
        expense.getOccurredAt(),
        expense.getNote(),
        expense.getCategory(),
        expense.getCreatorId());

    eventPublisher.publishEventAsync(event);
  }

  // Statistics DTOs
  public record ExpenseStatistics(
      Long totalCount,
      BigDecimal totalAmount,
      List<CategoryTotal> categoryTotals) {
  }

  public record CategoryTotal(
      String category,
      Long count,
      BigDecimal totalAmount) {
  }
}
