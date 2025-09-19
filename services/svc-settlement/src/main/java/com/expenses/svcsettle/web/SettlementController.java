package com.expenses.svcsettle.web;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcsettle.dto.SettlementDto;
import com.expenses.svcsettle.service.SettlementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Settlements", description = "Settlement proposal and payment tracking operations")
public class SettlementController {

  private final SettlementService settlementService;

  @PostMapping("/proposals")
  @Operation(summary = "Create settlement proposal", description = "Create a new settlement proposal for debt optimization or manual settlement")
  @ApiResponse(responseCode = "201", description = "Settlement proposal created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid settlement proposal data")
  @ApiResponse(responseCode = "403", description = "Not authorized to create settlement in this group")
  public ResponseEntity<SettlementDto.SettlementProposalResponse> createSettlementProposal(
      @Valid @RequestBody SettlementDto.CreateSettlementProposalRequest request,
      Authentication authentication) {

    log.info("Creating settlement proposal for group {}", request.groupId());

    SettlementDto.SettlementProposalResponse response = settlementService.createSettlementProposal(request,
        authentication);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/proposals/{proposalId}")
  @Operation(summary = "Get settlement proposal", description = "Get detailed information about a specific settlement proposal")
  @ApiResponse(responseCode = "200", description = "Settlement proposal found")
  @ApiResponse(responseCode = "404", description = "Settlement proposal not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to view this settlement proposal")
  public ResponseEntity<SettlementDto.SettlementProposalResponse> getSettlementProposal(
      @Parameter(description = "Settlement proposal ID") @PathVariable UUID proposalId,
      Authentication authentication) {

    SettlementDto.SettlementProposalResponse response = settlementService.getSettlementProposal(proposalId,
        authentication);

    return ResponseEntity.ok(response);
  }

  @PutMapping("/proposals/{proposalId}")
  @Operation(summary = "Update settlement proposal", description = "Update settlement proposal details (only allowed for pending proposals)")
  @ApiResponse(responseCode = "200", description = "Settlement proposal updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid settlement proposal data")
  @ApiResponse(responseCode = "404", description = "Settlement proposal not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to update this settlement proposal")
  public ResponseEntity<SettlementDto.SettlementProposalResponse> updateSettlementProposal(
      @Parameter(description = "Settlement proposal ID") @PathVariable UUID proposalId,
      @Valid @RequestBody SettlementDto.UpdateSettlementProposalRequest request,
      Authentication authentication) {

    log.info("Updating settlement proposal {}", proposalId);

    SettlementDto.SettlementProposalResponse response = settlementService.updateSettlementProposal(proposalId, request,
        authentication);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/proposals/{proposalId}/accept")
  @Operation(summary = "Accept settlement proposal", description = "Accept a pending settlement proposal")
  @ApiResponse(responseCode = "200", description = "Settlement proposal accepted successfully")
  @ApiResponse(responseCode = "404", description = "Settlement proposal not found")
  @ApiResponse(responseCode = "400", description = "Settlement proposal cannot be accepted")
  @ApiResponse(responseCode = "403", description = "Not authorized to accept this settlement proposal")
  public ResponseEntity<SettlementDto.SettlementProposalResponse> acceptSettlementProposal(
      @Parameter(description = "Settlement proposal ID") @PathVariable UUID proposalId,
      Authentication authentication) {

    log.info("Accepting settlement proposal {}", proposalId);

    SettlementDto.SettlementProposalResponse response = settlementService.acceptSettlementProposal(proposalId,
        authentication);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/proposals/{proposalId}/reject")
  @Operation(summary = "Reject settlement proposal", description = "Reject a pending settlement proposal")
  @ApiResponse(responseCode = "200", description = "Settlement proposal rejected successfully")
  @ApiResponse(responseCode = "404", description = "Settlement proposal not found")
  @ApiResponse(responseCode = "400", description = "Settlement proposal cannot be rejected")
  @ApiResponse(responseCode = "403", description = "Not authorized to reject this settlement proposal")
  public ResponseEntity<SettlementDto.SettlementProposalResponse> rejectSettlementProposal(
      @Parameter(description = "Settlement proposal ID") @PathVariable UUID proposalId,
      Authentication authentication) {

    log.info("Rejecting settlement proposal {}", proposalId);

    SettlementDto.SettlementProposalResponse response = settlementService.rejectSettlementProposal(proposalId,
        authentication);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/proposals/{proposalId}")
  @Operation(summary = "Cancel settlement proposal", description = "Cancel a settlement proposal (sets status to CANCELLED)")
  @ApiResponse(responseCode = "204", description = "Settlement proposal cancelled successfully")
  @ApiResponse(responseCode = "404", description = "Settlement proposal not found")
  @ApiResponse(responseCode = "400", description = "Settlement proposal cannot be cancelled")
  @ApiResponse(responseCode = "403", description = "Not authorized to cancel this settlement proposal")
  public ResponseEntity<Void> cancelSettlementProposal(
      @Parameter(description = "Settlement proposal ID") @PathVariable UUID proposalId,
      Authentication authentication) {

    log.info("Cancelling settlement proposal {}", proposalId);

    settlementService.cancelSettlementProposal(proposalId, authentication);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/proposals/group/{groupId}")
  @Operation(summary = "Get group settlement proposals", description = "Get all settlement proposals for a specific group")
  @ApiResponse(responseCode = "200", description = "Group settlement proposals retrieved")
  @ApiResponse(responseCode = "403", description = "Not authorized to view settlements for this group")
  public ResponseEntity<Page<SettlementDto.SettlementProposalSummary>> getGroupSettlementProposals(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<SettlementDto.SettlementProposalSummary> proposals = settlementService.getGroupSettlementProposals(
        groupId, status, pageable, authentication);

    return ResponseEntity.ok(proposals);
  }

  @GetMapping("/proposals/my")
  @Operation(summary = "Get my settlement proposals", description = "Get settlement proposals where current user is involved")
  @ApiResponse(responseCode = "200", description = "User settlement proposals retrieved")
  public ResponseEntity<Page<SettlementDto.SettlementProposalSummary>> getMySettlementProposals(
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<SettlementDto.SettlementProposalSummary> proposals = settlementService.getMySettlementProposals(
        pageable, authentication);

    return ResponseEntity.ok(proposals);
  }

  @PostMapping("/payments/{paymentId}/confirm-payer")
  @Operation(summary = "Confirm payment by payer", description = "Payer confirms that payment has been sent")
  @ApiResponse(responseCode = "200", description = "Payment confirmed by payer successfully")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "400", description = "Payment cannot be confirmed")
  @ApiResponse(responseCode = "403", description = "Not authorized to confirm this payment")
  public ResponseEntity<SettlementDto.SettlementPaymentResponse> confirmPaymentByPayer(
      @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
      @Valid @RequestBody SettlementDto.PaymentConfirmationRequest request,
      Authentication authentication) {

    log.info("Confirming payment {} by payer", paymentId);

    SettlementDto.SettlementPaymentResponse response = settlementService.confirmPaymentByPayer(
        paymentId, request, authentication);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/payments/{paymentId}/confirm-payee")
  @Operation(summary = "Confirm payment by payee", description = "Payee confirms that payment has been received")
  @ApiResponse(responseCode = "200", description = "Payment confirmed by payee successfully")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "400", description = "Payment cannot be confirmed")
  @ApiResponse(responseCode = "403", description = "Not authorized to confirm this payment")
  public ResponseEntity<SettlementDto.SettlementPaymentResponse> confirmPaymentByPayee(
      @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
      @Valid @RequestBody SettlementDto.PaymentConfirmationRequest request,
      Authentication authentication) {

    log.info("Confirming payment {} by payee", paymentId);

    SettlementDto.SettlementPaymentResponse response = settlementService.confirmPaymentByPayee(
        paymentId, request, authentication);

    return ResponseEntity.ok(response);
  }

  @PostMapping("/payments/{paymentId}/dispute")
  @Operation(summary = "Dispute payment", description = "Raise a dispute for a payment")
  @ApiResponse(responseCode = "200", description = "Payment disputed successfully")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to dispute this payment")
  public ResponseEntity<SettlementDto.SettlementPaymentResponse> disputePayment(
      @Parameter(description = "Payment ID") @PathVariable UUID paymentId,
      @Valid @RequestBody SettlementDto.DisputePaymentRequest request,
      Authentication authentication) {

    log.info("Disputing payment {}", paymentId);

    SettlementDto.SettlementPaymentResponse response = settlementService.disputePayment(
        paymentId, request, authentication);

    return ResponseEntity.ok(response);
  }

  @GetMapping("/payments/my")
  @Operation(summary = "Get my payments", description = "Get payments where current user is payer or payee")
  @ApiResponse(responseCode = "200", description = "User payments retrieved")
  public ResponseEntity<Page<SettlementDto.SettlementPaymentResponse>> getMyPayments(
      @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<SettlementDto.SettlementPaymentResponse> payments = settlementService.getMyPayments(
        status, pageable, authentication);

    return ResponseEntity.ok(payments);
  }

  @PostMapping("/optimize")
  @Operation(summary = "Optimize group debts", description = "Calculate and optionally create optimized debt settlement proposal")
  @ApiResponse(responseCode = "200", description = "Debt optimization calculated successfully")
  @ApiResponse(responseCode = "403", description = "Not authorized to optimize debts for this group")
  public ResponseEntity<SettlementDto.DebtOptimizationResult> optimizeDebts(
      @Valid @RequestBody SettlementDto.DebtOptimizationRequest request,
      Authentication authentication) {

    log.info("Optimizing debts for group {}", request.groupId());

    SettlementDto.DebtOptimizationResult result = settlementService.optimizeDebts(request, authentication);

    return ResponseEntity.ok(result);
  }
}
