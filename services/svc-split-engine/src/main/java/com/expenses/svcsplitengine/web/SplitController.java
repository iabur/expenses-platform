package com.expenses.svcsplitengine.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

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
import com.expenses.svcsplitengine.service.BalanceUpdateService;
import com.expenses.svcsplitengine.service.SplitCalculationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/splits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Split Engine", description = "Expense split calculation and balance management")
public class SplitController {

  private final SplitCalculationService splitCalculationService;
  private final BalanceUpdateService balanceUpdateService;

  @PostMapping("/calculate")
  @Operation(summary = "Calculate expense splits", description = "Calculate how an expense should be split among participants")
  @ApiResponse(responseCode = "200", description = "Splits calculated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid split request")
  public ResponseEntity<SplitResult> calculateSplits(
      @Valid @RequestBody SplitRequest request,
      Authentication authentication) {

    log.info("Calculating splits for expense {} using method {}",
        request.expenseId(), request.splitMethod());

    SplitResult result = splitCalculationService.calculateSplits(request);

    return ResponseEntity.ok(result);
  }

  @GetMapping("/expense/{expenseId}")
  @Operation(summary = "Get expense split calculation", description = "Retrieve the split calculation for a specific expense")
  @ApiResponse(responseCode = "200", description = "Split calculation found")
  @ApiResponse(responseCode = "404", description = "Split calculation not found")
  public ResponseEntity<SplitResult> getExpenseSplit(
      @Parameter(description = "Expense ID") @PathVariable UUID expenseId,
      Authentication authentication) {

    SplitResult result = splitCalculationService.getSplitCalculation(expenseId);

    if (result != null) {
      return ResponseEntity.ok(result);
    } else {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/balances/group/{groupId}")
  @Operation(summary = "Get group balances", description = "Get all user balances in a specific group")
  @ApiResponse(responseCode = "200", description = "Group balances retrieved")
  public ResponseEntity<List<GroupBalance>> getGroupBalances(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    List<GroupBalance> balances = balanceUpdateService.getGroupBalances(groupId);

    return ResponseEntity.ok(balances);
  }

  @GetMapping("/balances/group/{groupId}/debtors")
  @Operation(summary = "Get group debtors", description = "Get users who owe money in the group")
  @ApiResponse(responseCode = "200", description = "Debtors retrieved")
  public ResponseEntity<List<GroupBalance>> getGroupDebtors(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    List<GroupBalance> debtors = balanceUpdateService.getDebtors(groupId);

    return ResponseEntity.ok(debtors);
  }

  @GetMapping("/balances/group/{groupId}/creditors")
  @Operation(summary = "Get group creditors", description = "Get users who are owed money in the group")
  @ApiResponse(responseCode = "200", description = "Creditors retrieved")
  public ResponseEntity<List<GroupBalance>> getGroupCreditors(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    List<GroupBalance> creditors = balanceUpdateService.getCreditors(groupId);

    return ResponseEntity.ok(creditors);
  }

  @GetMapping("/balances/group/{groupId}/user/{userId}")
  @Operation(summary = "Get user balance in group", description = "Get specific user's balance in a group")
  @ApiResponse(responseCode = "200", description = "User balance retrieved")
  public ResponseEntity<GroupBalance> getUserBalance(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Parameter(description = "User ID") @PathVariable UUID userId,
      Authentication authentication) {

    // For simplicity, assuming USD currency. In real app, this should be
    // configurable
    GroupBalance balance = balanceUpdateService.getUserBalance(groupId, userId, "USD");

    return ResponseEntity.ok(balance);
  }
}
