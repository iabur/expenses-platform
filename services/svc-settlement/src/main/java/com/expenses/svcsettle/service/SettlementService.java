package com.expenses.svcsettle.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcsettle.dto.SettlementDto;
import com.expenses.svcsettle.entity.SettlementProposal;
import com.expenses.svcsettle.exception.SettlementNotFoundException;
import com.expenses.svcsettle.exception.UnauthorizedSettlementAccessException;
import com.expenses.svcsettle.repository.SettlementProposalRepository;
import com.expenses.svcsettle.repository.SettlementPaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SettlementService {

  private final SettlementProposalRepository settlementProposalRepository;
  private final SettlementPaymentRepository settlementPaymentRepository;

  /**
   * Create a new settlement proposal
   */
  public SettlementDto.SettlementProposalResponse createSettlementProposal(
      SettlementDto.CreateSettlementProposalRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    log.info("Creating settlement proposal for group {} by user {}", request.groupId(), currentUserId);

    // TODO: Verify user is member of the group

    SettlementProposal proposal = SettlementProposal.builder()
        .groupId(request.groupId())
        .proposerId(currentUserId)
        .title(request.title())
        .description(request.description())
        .currency(request.currency())
        .proposalType(request.proposalType())
        .expiresAt(request.expiresAt())
        .build();

    // TODO: Add payments from request

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

    log.info("Created settlement proposal {} for group {}", savedProposal.getId(), request.groupId());

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

    // TODO: Update payments from request

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

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

    // TODO: Check if user has permission to accept (group member or involved in
    // payments)

    proposal.accept();

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

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

    // TODO: Check if user has permission to reject

    proposal.reject();

    SettlementProposal savedProposal = settlementProposalRepository.save(proposal);

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

    // TODO: Verify user is member of the group

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

    // TODO: Implement payment confirmation logic

    log.info("Payment {} confirmed by payer {}", paymentId, currentUserId);

    // Return placeholder response
    return SettlementDto.SettlementPaymentResponse.builder()
        .id(paymentId)
        .build();
  }

  /**
   * Confirm payment by payee
   */
  public SettlementDto.SettlementPaymentResponse confirmPaymentByPayee(
      UUID paymentId,
      SettlementDto.PaymentConfirmationRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Implement payment confirmation logic

    log.info("Payment {} confirmed by payee {}", paymentId, currentUserId);

    // Return placeholder response
    return SettlementDto.SettlementPaymentResponse.builder()
        .id(paymentId)
        .build();
  }

  /**
   * Dispute payment
   */
  public SettlementDto.SettlementPaymentResponse disputePayment(
      UUID paymentId,
      SettlementDto.DisputePaymentRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Implement payment dispute logic

    log.info("Payment {} disputed by user {}", paymentId, currentUserId);

    // Return placeholder response
    return SettlementDto.SettlementPaymentResponse.builder()
        .id(paymentId)
        .build();
  }

  /**
   * Get payments where user is involved
   */
  @Transactional(readOnly = true)
  public Page<SettlementDto.SettlementPaymentResponse> getMyPayments(String status, Pageable pageable,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Implement payment retrieval logic

    // Return empty page as placeholder
    return Page.empty(pageable);
  }

  /**
   * Optimize debts for a group
   */
  public SettlementDto.DebtOptimizationResult optimizeDebts(
      SettlementDto.DebtOptimizationRequest request,
      Authentication authentication) {

    UUID currentUserId = getCurrentUserId(authentication);

    // TODO: Implement debt optimization algorithm

    log.info("Optimizing debts for group {} by user {}", request.groupId(), currentUserId);

    // Return placeholder result
    return SettlementDto.DebtOptimizationResult.builder()
        .groupId(request.groupId())
        .currency(request.currency())
        .build();
  }

  // Helper methods

  private UUID getCurrentUserId(Authentication authentication) {
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return UUID.fromString(jwt.getSubject());
    }
    throw new IllegalArgumentException("Invalid authentication type");
  }
}
