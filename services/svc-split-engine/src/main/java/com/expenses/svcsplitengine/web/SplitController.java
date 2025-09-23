package com.expenses.svcsplitengine.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcsplitengine.dto.SplitRequest;
import com.expenses.svcsplitengine.dto.SplitResult;
import com.expenses.svcsplitengine.entity.GroupBalance;
import com.expenses.svcsplitengine.entity.SplitCalculation;
import com.expenses.svcsplitengine.service.BalanceUpdateService;
import com.expenses.svcsplitengine.service.SplitCalculationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/splits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Split Engine", description = "Expense split calculations and balance management")
public class SplitController {

  private final SplitCalculationService splitCalculationService;
  private final BalanceUpdateService balanceUpdateService;

  @PostMapping("/calculate")
  @Operation(summary = "Calculate expense split", description = "Calculate how an expense should be split among participants")
  @ApiResponse(responseCode = "200", description = "Split calculated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid split request")
  @ApiResponse(responseCode = "401", description = "User not authenticated")
  public ResponseEntity<SplitResult> calculateSplit(
      @Valid @RequestBody SplitRequest request,
      Authentication authentication) {

    log.info("Calculating split for expense: {}", request.expenseId());

    // TODO: Implement calculateSplit method in service
    SplitResult result = SplitResult.builder()
        .calculationId(UUID.randomUUID())
        .expenseId(request.expenseId())
        .groupId(request.groupId())
        .totalAmountCents(request.totalAmountCents())
        .currency(request.currency())
        .splitMethod(request.splitMethod())
        .status(SplitCalculation.CalculationStatus.PENDING)
        .calculatedAt(java.time.ZonedDateTime.now())
        .participantSplits(List.of())
        .build();

    return ResponseEntity.ok(result);
  }

  @GetMapping("/group/{groupId}/balances")
  @Operation(summary = "Get group balances", description = "Get current balances for all users in a group")
  @ApiResponse(responseCode = "200", description = "Group balances retrieved successfully")
  @ApiResponse(responseCode = "403", description = "Not authorized to view group balances")
  @ApiResponse(responseCode = "404", description = "Group not found")
  public ResponseEntity<List<GroupBalance>> getGroupBalances(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    log.info("Getting balances for group: {}", groupId);

    List<GroupBalance> balances = balanceUpdateService.getGroupBalances(groupId);

    log.info("Found {} balances for group {}", balances.size(), groupId);

    return ResponseEntity.ok(balances);
  }

  @GetMapping("/user/{userId}/balances")
  @Operation(summary = "Get user balances", description = "Get balances for a specific user across all groups")
  @ApiResponse(responseCode = "200", description = "User balances retrieved successfully")
  @ApiResponse(responseCode = "403", description = "Not authorized to view user balances")
  @ApiResponse(responseCode = "404", description = "User not found")
  public ResponseEntity<Page<GroupBalance>> getUserBalances(
      @Parameter(description = "User ID") @PathVariable UUID userId,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    // TODO: Implement getUserBalances method in service
    Page<GroupBalance> balances = Page.empty(pageable);

    return ResponseEntity.ok(balances);
  }

  @GetMapping("/group/{groupId}/debts")
  @Operation(summary = "Get group debt summary", description = "Get simplified debt relationships within a group")
  @ApiResponse(responseCode = "200", description = "Group debts retrieved successfully")
  @ApiResponse(responseCode = "403", description = "Not authorized to view group debts")
  @ApiResponse(responseCode = "404", description = "Group not found")
  public ResponseEntity<List<Object>> getGroupDebts(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    // This would return simplified debt relationships
    // For now, return empty list as placeholder
    return ResponseEntity.ok(List.of());
  }

  @PostMapping("/group/{groupId}/optimize")
  @Operation(summary = "Optimize group debts", description = "Calculate optimized debt settlement for a group")
  @ApiResponse(responseCode = "200", description = "Debt optimization calculated successfully")
  @ApiResponse(responseCode = "403", description = "Not authorized to optimize group debts")
  @ApiResponse(responseCode = "404", description = "Group not found")
  public ResponseEntity<Object> optimizeGroupDebts(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    log.info("Optimizing debts for group: {}", groupId);

    // This would calculate and return optimized debt settlement
    // For now, return empty object as placeholder
    return ResponseEntity.ok(new Object());
  }
}