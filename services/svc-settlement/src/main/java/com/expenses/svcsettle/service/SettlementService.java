package com.expenses.svcsettle.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcsettle.client.GroupServiceClient;
import com.expenses.svcsettle.dto.SettlementDto;
import com.expenses.svcsettle.entity.SettlementPayment;
import com.expenses.svcsettle.entity.SettlementProposal;
import com.expenses.svcsettle.exception.SettlementNotFoundException;
import com.expenses.svcsettle.exception.UnauthorizedSettlementAccessException;
import com.expenses.svcsettle.repository.SettlementPaymentRepository;
import com.expenses.svcsettle.repository.SettlementProposalRepository;
import com.expenses.svcsettle.service.SettlementHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

  private final SettlementProposalRepository settlementProposalRepository;
  private final SettlementPaymentRepository settlementPaymentRepository;
  private final SettlementHistoryService settlementHistoryService;
  private final GroupServiceClient groupServiceClient;

  /**
   * Create a new settlement proposal
   */
  public SettlementDto.SettlementProposalResponse createSettlementProposal(
      SettlementDto.CreateSettlementProposalRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    log.info("Creating settlement proposal for group {} by user {}", request.groupId(), currentUserId);

    // Verify user is member of the group
    if (!isUserMemberOfGroup(request.groupId(), currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to create settlement proposals");
    }

    // Calculate total amount from payments first
    long totalAmountCents = request.payments().stream()
        .mapToLong(paymentRequest -> paymentRequest.amount().multiply(BigDecimal.valueOf(100)).longValue())
        .sum();

    SettlementProposal proposal = SettlementProposal.builder()
        .groupId(request.groupId())
        .proposerId(currentUserId)
        .title(request.title())
        .description(request.description())
        .currency(request.currency())
        .proposalType(request.proposalType())
        .expiresAt(request.expiresAt())
        .totalAmountCents(totalAmountCents)
        .build();

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

    // Add payments from request
    final SettlementProposal finalProposal = savedProposal;
    List<SettlementPayment> payments = request.payments().stream()
        .map(paymentRequest -> SettlementPayment.builder()
            .proposal(finalProposal)
            .payerId(paymentRequest.payerId())
            .payeeId(paymentRequest.payeeId())
            .currency(request.currency())
            .amountCents(paymentRequest.amount().multiply(BigDecimal.valueOf(100)).longValue())
            .description(paymentRequest.description())
            .paymentMethod(paymentRequest.paymentMethod())
            .status(SettlementPayment.PaymentStatus.PENDING)
            .build())
        .toList();

    settlementPaymentRepository.saveAll(payments);

    // Attach payments to proposal entity so response includes them (the owning side is SettlementPayment)
    payments.forEach(p -> savedProposal.getPayments().add(p));

    // Record history
    settlementHistoryService.recordProposalCreated(savedProposal, currentUserId);

    log.info("Created settlement proposal {} for group {} with {} payments", savedProposal.getId(), request.groupId(), payments.size());

    return SettlementDto.SettlementProposalResponse.from(savedProposal);
  }

  /**
   * Get settlement proposal by ID
   */
  @Transactional(readOnly = true)
  public SettlementDto.SettlementProposalResponse getSettlementProposal(UUID proposalId,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if user has access to this proposal
    if (!settlementProposalRepository.hasUserAccess(proposalId, currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You don't have access to this settlement proposal");
    }

    return SettlementDto.SettlementProposalResponse.from(proposal);
  }

  /**
   * Update settlement proposal
   */
  public SettlementDto.SettlementProposalResponse updateSettlementProposal(
      UUID proposalId,
      SettlementDto.UpdateSettlementProposalRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if user can modify this proposal (only proposer)
    if (!proposal.getProposerId().equals(currentUserId)) {
      throw new UnauthorizedSettlementAccessException("Only the proposer can modify this settlement proposal");
    }

    // Check if proposal can be modified
    if (!proposal.canBeModified()) {
      throw new IllegalStateException("Settlement proposal cannot be modified in current state");
    }

    // Update proposal
    proposal.setTitle(request.title());
    proposal.setDescription(request.description());
    proposal.setExpiresAt(request.expiresAt());

    // Update payments from request if provided
    if (request.payments() != null && !request.payments().isEmpty()) {
      // Remove existing payments
      proposal.getPayments().clear();
      
      // Add new payments
      List<SettlementPayment> newPayments = request.payments().stream()
          .map(paymentRequest -> SettlementPayment.builder()
              .proposal(proposal)
              .payerId(paymentRequest.payerId())
              .payeeId(paymentRequest.payeeId())
              .currency(proposal.getCurrency())
              .amountCents(paymentRequest.amount().multiply(BigDecimal.valueOf(100)).longValue())
              .description(paymentRequest.description())
              .paymentMethod(paymentRequest.paymentMethod())
              .status(SettlementPayment.PaymentStatus.PENDING)
              .build())
          .toList();
      
      proposal.getPayments().addAll(newPayments);
      
      // Recalculate total amount
      long newTotalAmountCents = newPayments.stream()
          .mapToLong(SettlementPayment::getAmountCents)
          .sum();
      proposal.setTotalAmountCents(newTotalAmountCents);
    }

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

    // Record history
    settlementHistoryService.recordProposalUpdated(savedProposal, currentUserId);

    log.info("Updated settlement proposal {} by user {}", proposalId, currentUserId);

    return SettlementDto.SettlementProposalResponse.from(savedProposal);
  }

  /**
   * Accept settlement proposal
   */
  public SettlementDto.SettlementProposalResponse acceptSettlementProposal(UUID proposalId,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if user has permission to accept (group member or involved in payments)
    if (!isUserMemberOfGroup(proposal.getGroupId(), currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to accept settlement proposals");
    }

    proposal.accept();

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

    // Record history
    settlementHistoryService.recordProposalAccepted(savedProposal, currentUserId);

    log.info("Accepted settlement proposal {} by user {}", proposalId, currentUserId);

    return SettlementDto.SettlementProposalResponse.from(savedProposal);
  }

  /**
   * Reject settlement proposal
   */
  public SettlementDto.SettlementProposalResponse rejectSettlementProposal(UUID proposalId,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if user has permission to reject
    if (!isUserMemberOfGroup(proposal.getGroupId(), currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to reject settlement proposals");
    }

    proposal.reject();

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

    // Record history
    settlementHistoryService.recordProposalRejected(savedProposal, currentUserId);

    log.info("Rejected settlement proposal {} by user {}", proposalId, currentUserId);

    return SettlementDto.SettlementProposalResponse.from(savedProposal);
  }

  /**
   * Cancel settlement proposal
   */
  public void cancelSettlementProposal(UUID proposalId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if user can cancel this proposal (only proposer)
    if (!proposal.getProposerId().equals(currentUserId)) {
      throw new UnauthorizedSettlementAccessException("Only the proposer can cancel this settlement proposal");
    }

    proposal.cancel();

    settlementProposalRepository.save(proposal);

    log.info("Cancelled settlement proposal {} by user {}", proposalId, currentUserId);
  }

  /**
   * Get settlement proposals for a group
   */
  @Transactional(readOnly = true)
  public Page<SettlementDto.SettlementProposalSummary> getGroupSettlementProposals(
      UUID groupId, String status, Pageable pageable, Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // Verify user is member of the group
    if (!isUserMemberOfGroup(groupId, currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to view settlement proposals");
    }

    if (status != null) {
      SettlementProposal.SettlementStatus settlementStatus = SettlementProposal.SettlementStatus
          .valueOf(status.toUpperCase());
      return settlementProposalRepository
          .findByGroupIdAndStatusOrderByCreatedAtDesc(groupId, settlementStatus, pageable)
          .map(SettlementDto.SettlementProposalSummary::from);
    } else {
      return settlementProposalRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable)
          .map(SettlementDto.SettlementProposalSummary::from);
    }
  }

  /**
   * Get settlement proposals where user is involved
   */
  @Transactional(readOnly = true)
  public Page<SettlementDto.SettlementProposalSummary> getMySettlementProposals(Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    return settlementProposalRepository.findByUserInvolvement(currentUserId, pageable)
        .map(SettlementDto.SettlementProposalSummary::from);
  }

  /**
   * Confirm payment by payer
   */
  public SettlementDto.SettlementPaymentResponse confirmPaymentByPayer(
      UUID paymentId,
      SettlementDto.PaymentConfirmationRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
        .orElseThrow(() -> new SettlementNotFoundException("Payment not found: " + paymentId));

    // Verify user is the payer
    if (!payment.getPayerId().equals(currentUserId)) {
      throw new IllegalArgumentException("Only the payer can confirm payment");
    }

    // Confirm payment by payer
    payment.confirmByPayer(currentUserId, request.notes(), request.attachmentUrl());

    // Update payment reference if provided
    if (request.paymentReference() != null && !request.paymentReference().trim().isEmpty()) {
      payment.setPaymentReference(request.paymentReference());
    }

    SettlementPayment savedPayment = settlementPaymentRepository.save(payment);

    // Record history
    settlementHistoryService.recordPaymentConfirmed(savedPayment, currentUserId, "PAYER");

    log.info("Payment {} confirmed by payer {}", paymentId, currentUserId);

    return SettlementDto.SettlementPaymentResponse.from(savedPayment);
  }

  /**
   * Confirm payment by payee
   */
  public SettlementDto.SettlementPaymentResponse confirmPaymentByPayee(
      UUID paymentId,
      SettlementDto.PaymentConfirmationRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
        .orElseThrow(() -> new SettlementNotFoundException("Payment not found: " + paymentId));

    // Verify user is the payee
    if (!payment.getPayeeId().equals(currentUserId)) {
      throw new IllegalArgumentException("Only the payee can confirm payment receipt");
    }

    // Confirm payment by payee
    payment.confirmByPayee(currentUserId, request.notes());

    SettlementPayment savedPayment = settlementPaymentRepository.save(payment);

    // Record history
    settlementHistoryService.recordPaymentConfirmed(savedPayment, currentUserId, "PAYEE");

    // Check if all payments in the proposal are completed
    checkAndUpdateProposalCompletion(payment.getProposal().getId());

    log.info("Payment {} confirmed by payee {}", paymentId, currentUserId);

    return SettlementDto.SettlementPaymentResponse.from(savedPayment);
  }

  /**
   * Dispute payment
   */
  public SettlementDto.SettlementPaymentResponse disputePayment(
      UUID paymentId,
      SettlementDto.DisputePaymentRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
        .orElseThrow(() -> new SettlementNotFoundException("Payment not found: " + paymentId));

    // Check if user has permission to dispute (payer or payee)
    if (!payment.getPayerId().equals(currentUserId) && !payment.getPayeeId().equals(currentUserId)) {
      throw new UnauthorizedSettlementAccessException("Only the payer or payee can dispute this payment");
    }

    // Check if payment can be disputed (not already completed or disputed)
    if (payment.getStatus() == SettlementPayment.PaymentStatus.COMPLETED) {
      throw new IllegalStateException("Cannot dispute a completed payment");
    }

    if (payment.isDisputed()) {
      throw new IllegalStateException("Payment is already disputed");
    }

    // Create dispute
    payment.dispute(currentUserId, request.reason());

    SettlementPayment savedPayment = settlementPaymentRepository.save(payment);

    // Record history
    settlementHistoryService.recordPaymentDisputed(savedPayment, currentUserId, request.reason());

    log.info("Payment {} disputed by user {} with reason: {}", paymentId, currentUserId, request.reason());

    return SettlementDto.SettlementPaymentResponse.from(savedPayment);
  }

  /**
   * Get payments where user is involved
   */
  @Transactional(readOnly = true)
  public Page<SettlementDto.SettlementPaymentResponse> getMyPayments(String status, Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Page<SettlementPayment> payments;

    if (status != null && !status.trim().isEmpty()) {
      // Filter by specific status
      try {
        SettlementPayment.PaymentStatus paymentStatus = SettlementPayment.PaymentStatus.valueOf(status.toUpperCase());
        payments = settlementPaymentRepository.findByUserInvolvement(currentUserId, pageable)
            .map(payment -> payment.getStatus() == paymentStatus ? payment : null)
            .map(payment -> payment != null ? payment : null);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid payment status: " + status);
      }
    } else {
      // Get all payments for user
      payments = settlementPaymentRepository.findByUserInvolvement(currentUserId, pageable);
    }

    return payments.map(SettlementDto.SettlementPaymentResponse::from);
  }

  /**
   * Optimize debts for a group
   */
  public SettlementDto.DebtOptimizationResult optimizeDebts(
      SettlementDto.DebtOptimizationRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // Verify user is member of the group
    if (!isUserMemberOfGroup(request.groupId(), currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to optimize debts");
    }

    log.info("Optimizing debts for group {} by user {}", request.groupId(), currentUserId);

    // Get all active proposals for the group
    List<SettlementProposal> activeProposals = settlementProposalRepository.findByGroupIdOrderByCreatedAtDesc(request.groupId(), Pageable.unpaged())
        .getContent()
        .stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.ACCEPTED)
        .toList();

    // Calculate current debt relationships
    Map<UUID, BigDecimal> netBalances = calculateNetBalances(activeProposals);
    
    // Generate optimized transactions
    List<SettlementDto.OptimizedTransaction> optimizedTransactions = generateOptimizedTransactions(netBalances, request.minimumAmount());

    // Calculate statistics
    int originalTransactionsCount = activeProposals.stream()
        .mapToInt(p -> p.getPayments().size())
        .sum();
    
    int optimizedTransactionsCount = optimizedTransactions.size();
    
    BigDecimal savingsPercentage = originalTransactionsCount > 0 
        ? BigDecimal.valueOf((double)(originalTransactionsCount - optimizedTransactionsCount) / originalTransactionsCount * 100)
        : BigDecimal.ZERO;

    BigDecimal totalDebtAmount = netBalances.values().stream()
        .filter(balance -> balance.compareTo(BigDecimal.ZERO) > 0)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    return SettlementDto.DebtOptimizationResult.builder()
        .groupId(request.groupId())
        .currency(request.currency())
        .totalDebtAmount(totalDebtAmount)
        .originalTransactionsCount(originalTransactionsCount)
        .optimizedTransactionsCount(optimizedTransactionsCount)
        .savingsPercentage(savingsPercentage)
        .optimizedTransactions(optimizedTransactions)
        .build();
  }

  /**
   * Get group settlement status and debt summary
   */
  @Transactional(readOnly = true)
  public SettlementDto.GroupSettlementStatus getGroupSettlementStatus(
      UUID groupId, Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // Verify user is member of the group
    if (!isUserMemberOfGroup(groupId, currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to view settlement status");
    }

    log.info("Getting settlement status for group {} by user {}", groupId, currentUserId);

    // Get all proposals for the group
    List<SettlementProposal> allProposals = settlementProposalRepository.findByGroupIdOrderByCreatedAtDesc(groupId, Pageable.unpaged()).getContent();

    // Calculate totals
    BigDecimal totalDebtAmount = allProposals.stream()
        .map(SettlementProposal::getTotalAmountDecimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal totalSettledAmount = allProposals.stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.COMPLETED)
        .map(SettlementProposal::getTotalAmountDecimal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal remainingDebtAmount = totalDebtAmount.subtract(totalSettledAmount);

    // Count proposals by status
    long activeProposalsCount = allProposals.stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.ACCEPTED)
        .count();

    long completedProposalsCount = allProposals.stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.COMPLETED)
        .count();

    // Count payments by status
    long pendingPaymentsCount = allProposals.stream()
        .flatMap(p -> p.getPayments().stream())
        .filter(p -> p.getStatus() == SettlementPayment.PaymentStatus.PENDING)
        .count();

    long completedPaymentsCount = allProposals.stream()
        .flatMap(p -> p.getPayments().stream())
        .filter(p -> p.getStatus() == SettlementPayment.PaymentStatus.COMPLETED)
        .count();

    long disputedPaymentsCount = allProposals.stream()
        .flatMap(p -> p.getPayments().stream())
        .filter(SettlementPayment::isDisputed)
        .count();

    // Get active proposals
    List<SettlementDto.SettlementProposalSummary> activeProposals = allProposals.stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.ACCEPTED)
        .map(SettlementDto.SettlementProposalSummary::from)
        .toList();

    // Calculate debt summary per user
    List<SettlementDto.DebtSummary> debtSummary = calculateDebtSummary(allProposals);

    // Find last settlement date
    java.time.ZonedDateTime lastSettlementAt = allProposals.stream()
        .filter(p -> p.getStatus() == SettlementProposal.SettlementStatus.COMPLETED)
        .map(SettlementProposal::getUpdatedAt)
        .max(java.time.ZonedDateTime::compareTo)
        .orElse(null);

    return SettlementDto.GroupSettlementStatus.builder()
        .groupId(groupId)
        .currency(getGroupCurrency(groupId))
        .totalDebtAmount(totalDebtAmount)
        .totalSettledAmount(totalSettledAmount)
        .remainingDebtAmount(remainingDebtAmount)
        .activeProposalsCount((int) activeProposalsCount)
        .completedProposalsCount((int) completedProposalsCount)
        .pendingPaymentsCount((int) pendingPaymentsCount)
        .completedPaymentsCount((int) completedPaymentsCount)
        .disputedPaymentsCount((int) disputedPaymentsCount)
        .debtSummary(debtSummary)
        .activeProposals(activeProposals)
        .lastSettlementAt(lastSettlementAt)
        .calculatedAt(java.time.ZonedDateTime.now())
        .build();
  }

  /**
   * Get group settlement history
   */
  @Transactional(readOnly = true)
  public Page<SettlementDto.SettlementHistoryItem> getGroupSettlementHistory(
      UUID groupId, String status, Pageable pageable, Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // Verify user is member of the group
    if (!isUserMemberOfGroup(groupId, currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You must be a member of the group to view settlement history");
    }

    log.info("Getting settlement history for group {} by user {}", groupId, currentUserId);

    // Get settlement history from history service
    return settlementHistoryService.getGroupSettlementHistory(groupId, status, pageable);
  }

  /**
   * Get payment details
   */
  @Transactional(readOnly = true)
  public SettlementDto.SettlementPaymentResponse getPayment(
      UUID paymentId, Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    SettlementPayment payment = settlementPaymentRepository.findById(paymentId)
        .orElseThrow(() -> new SettlementNotFoundException("Payment not found: " + paymentId));

    // Check if user has access to this payment
    if (!payment.getPayerId().equals(currentUserId) && !payment.getPayeeId().equals(currentUserId)) {
      throw new UnauthorizedSettlementAccessException("You don't have access to this payment");
    }

    return SettlementDto.SettlementPaymentResponse.from(payment);
  }

  // Helper methods

  /**
   * Calculate net balances for all users from active proposals
   */
  private Map<UUID, BigDecimal> calculateNetBalances(List<SettlementProposal> proposals) {
    Map<UUID, BigDecimal> netBalances = new HashMap<>();
    
    for (SettlementProposal proposal : proposals) {
      for (SettlementPayment payment : proposal.getPayments()) {
        if (payment.getStatus() == SettlementPayment.PaymentStatus.PENDING ||
            payment.getStatus() == SettlementPayment.PaymentStatus.CONFIRMED) {
          
          UUID payerId = payment.getPayerId();
          UUID payeeId = payment.getPayeeId();
          BigDecimal amount = payment.getAmountDecimal();
          
          // Payer owes money (negative balance)
          netBalances.merge(payerId, amount.negate(), BigDecimal::add);
          
          // Payee is owed money (positive balance)
          netBalances.merge(payeeId, amount, BigDecimal::add);
        }
      }
    }
    
    return netBalances;
  }

  /**
   * Generate optimized transactions to minimize the number of payments
   */
  private List<SettlementDto.OptimizedTransaction> generateOptimizedTransactions(
      Map<UUID, BigDecimal> netBalances, BigDecimal minimumAmount) {
    
    List<SettlementDto.OptimizedTransaction> transactions = new ArrayList<>();
    
    // Separate creditors (positive balance) and debtors (negative balance)
    List<Map.Entry<UUID, BigDecimal>> creditors = netBalances.entrySet().stream()
        .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) > 0)
        .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
        .toList();
    
    List<Map.Entry<UUID, BigDecimal>> debtors = netBalances.entrySet().stream()
        .filter(entry -> entry.getValue().compareTo(BigDecimal.ZERO) < 0)
        .sorted((a, b) -> a.getValue().compareTo(b.getValue()))
        .toList();
    
    // Simple debt optimization: match largest creditors with largest debtors
    int creditorIndex = 0;
    int debtorIndex = 0;
    
    while (creditorIndex < creditors.size() && debtorIndex < debtors.size()) {
      Map.Entry<UUID, BigDecimal> creditor = creditors.get(creditorIndex);
      Map.Entry<UUID, BigDecimal> debtor = debtors.get(debtorIndex);
      
      BigDecimal creditorAmount = creditor.getValue();
      BigDecimal debtorAmount = debtor.getValue().abs();
      
      BigDecimal transferAmount = creditorAmount.min(debtorAmount);
      
      // Only create transaction if amount is above minimum threshold
      if (transferAmount.compareTo(minimumAmount) >= 0) {
        transactions.add(SettlementDto.OptimizedTransaction.builder()
            .payerId(debtor.getKey())
            .payeeId(creditor.getKey())
            .amount(transferAmount)
            .description("Optimized debt settlement")
            .build());
      }
      
      // Update balances
      creditor.setValue(creditorAmount.subtract(transferAmount));
      debtor.setValue(debtorAmount.subtract(transferAmount));
      
      // Move to next creditor/debtor if current one is settled
      if (creditor.getValue().compareTo(BigDecimal.ZERO) <= 0) {
        creditorIndex++;
      }
      if (debtor.getValue().compareTo(BigDecimal.ZERO) >= 0) {
        debtorIndex++;
      }
    }
    
    return transactions;
  }

  /**
   * Calculate debt summary for all users in the group
   */
  private List<SettlementDto.DebtSummary> calculateDebtSummary(List<SettlementProposal> proposals) {
    Map<UUID, BigDecimal> totalOwed = new HashMap<>();
    Map<UUID, BigDecimal> totalOwing = new HashMap<>();
    Map<UUID, Integer> activeDebtsCount = new HashMap<>();
    Map<UUID, Integer> activeCreditsCount = new HashMap<>();

    // Process all payments from all proposals
    for (SettlementProposal proposal : proposals) {
      for (SettlementPayment payment : proposal.getPayments()) {
        UUID payerId = payment.getPayerId();
        UUID payeeId = payment.getPayeeId();
        BigDecimal amount = payment.getAmountDecimal();

        // Only count active payments (not completed or cancelled)
        if (payment.getStatus() == SettlementPayment.PaymentStatus.PENDING ||
            payment.getStatus() == SettlementPayment.PaymentStatus.CONFIRMED) {
          
          // Payer owes money
          totalOwed.merge(payerId, amount, BigDecimal::add);
          activeDebtsCount.merge(payerId, 1, Integer::sum);

          // Payee is owed money
          totalOwing.merge(payeeId, amount, BigDecimal::add);
          activeCreditsCount.merge(payeeId, 1, Integer::sum);
        }
      }
    }

    // Create debt summary for all users involved
    Set<UUID> allUsers = new HashSet<>();
    allUsers.addAll(totalOwed.keySet());
    allUsers.addAll(totalOwing.keySet());

    return allUsers.stream()
        .map(userId -> {
          BigDecimal owed = totalOwed.getOrDefault(userId, BigDecimal.ZERO);
          BigDecimal owing = totalOwing.getOrDefault(userId, BigDecimal.ZERO);
          BigDecimal netBalance = owing.subtract(owed);

          return SettlementDto.DebtSummary.builder()
              .userId(userId)
              .totalOwed(owed)
              .totalOwing(owing)
              .netBalance(netBalance)
              .activeDebtsCount(activeDebtsCount.getOrDefault(userId, 0))
              .activeCreditsCount(activeCreditsCount.getOrDefault(userId, 0))
              .build();
        })
        .toList();
  }

  private UUID getCurrentUserId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return UUID.fromString(jwt.getSubject());
    }
    throw new IllegalArgumentException("Invalid authentication type");
  }

  /**
   * Check if user is member of group using Group Service
   */
  private boolean isUserMemberOfGroup(UUID groupId, UUID userId) {
    log.debug("Checking group membership for user {} in group {}", userId, groupId);
    return groupServiceClient.isUserMemberOfGroup(groupId, userId);
  }

  /**
   * Get group currency from Group Service
   */
  private String getGroupCurrency(UUID groupId) {
    log.debug("Getting currency for group {}", groupId);
    return groupServiceClient.getGroupCurrency(groupId);
  }

  /**
   * Check if all payments in a proposal are completed and update proposal status
   */
  private void checkAndUpdateProposalCompletion(UUID proposalId) {
    SettlementProposal proposal = settlementProposalRepository.findById(proposalId)
        .orElseThrow(() -> new SettlementNotFoundException("Settlement proposal not found: " + proposalId));

    // Check if all payments are completed
    boolean allPaymentsCompleted = proposal.getPayments().stream()
        .allMatch(payment -> payment.getStatus() == SettlementPayment.PaymentStatus.COMPLETED);

    if (allPaymentsCompleted && proposal.getStatus() == SettlementProposal.SettlementStatus.ACCEPTED) {
      // Update proposal status to COMPLETED
      proposal.setStatus(SettlementProposal.SettlementStatus.COMPLETED);
      SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

      // Record history
      settlementHistoryService.recordProposalCompleted(savedProposal);

      log.info("Settlement proposal {} marked as completed - all payments finished", proposalId);
    }
  }
}
