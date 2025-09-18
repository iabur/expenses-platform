package com.expenses.svcexpense.web;

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

import com.expenses.svcexpense.dto.ExpenseDto;
import com.expenses.svcexpense.service.ExpenseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expenses", description = "Expense management operations")
public class ExpenseController {

  private final ExpenseService expenseService;

  @PostMapping
  @Operation(summary = "Create expense", description = "Create a new expense in a group")
  @ApiResponse(responseCode = "201", description = "Expense created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid expense data")
  @ApiResponse(responseCode = "403", description = "Not authorized to create expense in this group")
  public ResponseEntity<ExpenseDto.ExpenseResponse> createExpense(
      @Valid @RequestBody ExpenseDto.CreateExpenseRequest request,
      Authentication authentication) {

    log.info("Creating expense for group {}", request.groupId());

    ExpenseDto.ExpenseResponse response = expenseService.createExpense(request, authentication);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{expenseId}")
  @Operation(summary = "Get expense", description = "Get expense details by ID")
  @ApiResponse(responseCode = "200", description = "Expense found")
  @ApiResponse(responseCode = "404", description = "Expense not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to view this expense")
  public ResponseEntity<ExpenseDto.ExpenseResponse> getExpense(
      @Parameter(description = "Expense ID") @PathVariable UUID expenseId,
      Authentication authentication) {

    ExpenseDto.ExpenseResponse response = expenseService.getExpense(expenseId, authentication);

    return ResponseEntity.ok(response);
  }

  @PutMapping("/{expenseId}")
  @Operation(summary = "Update expense", description = "Update expense details")
  @ApiResponse(responseCode = "200", description = "Expense updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid expense data")
  @ApiResponse(responseCode = "404", description = "Expense not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to update this expense")
  public ResponseEntity<ExpenseDto.ExpenseResponse> updateExpense(
      @Parameter(description = "Expense ID") @PathVariable UUID expenseId,
      @Valid @RequestBody ExpenseDto.UpdateExpenseRequest request,
      Authentication authentication) {

    log.info("Updating expense {}", expenseId);

    ExpenseDto.ExpenseResponse response = expenseService.updateExpense(expenseId, request, authentication);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{expenseId}")
  @Operation(summary = "Delete expense", description = "Soft delete an expense")
  @ApiResponse(responseCode = "204", description = "Expense deleted successfully")
  @ApiResponse(responseCode = "404", description = "Expense not found")
  @ApiResponse(responseCode = "403", description = "Not authorized to delete this expense")
  public ResponseEntity<Void> deleteExpense(
      @Parameter(description = "Expense ID") @PathVariable UUID expenseId,
      Authentication authentication) {

    log.info("Deleting expense {}", expenseId);

    expenseService.deleteExpense(expenseId, authentication);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/group/{groupId}")
  @Operation(summary = "Get group expenses", description = "Get all expenses for a specific group")
  @ApiResponse(responseCode = "200", description = "Group expenses retrieved")
  @ApiResponse(responseCode = "403", description = "Not authorized to view expenses for this group")
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getGroupExpenses(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getGroupExpenses(
        groupId, pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/my")
  @Operation(summary = "Get my expenses", description = "Get expenses created by current user")
  @ApiResponse(responseCode = "200", description = "User expenses retrieved")
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getMyExpenses(
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getUserExpenses(
        pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/participated")
  @Operation(summary = "Get participated expenses", description = "Get expenses where current user is a participant")
  @ApiResponse(responseCode = "200", description = "Participated expenses retrieved")
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getParticipatedExpenses(
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getUserParticipatedExpenses(
        pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/search")
  @Operation(summary = "Search expenses", description = "Search expenses by category or note")
  @ApiResponse(responseCode = "200", description = "Search results retrieved")
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> searchExpenses(
      @Parameter(description = "Search query") @RequestParam String query,
      @Parameter(description = "Group ID (optional)") @RequestParam(required = false) UUID groupId,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.searchExpenses(
        query, groupId, pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/group/{groupId}/statistics")
  @Operation(summary = "Get group expense statistics", description = "Get expense statistics for a group")
  @ApiResponse(responseCode = "200", description = "Statistics retrieved")
  @ApiResponse(responseCode = "403", description = "Not authorized to view statistics for this group")
  public ResponseEntity<ExpenseService.ExpenseStatistics> getGroupStatistics(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    ExpenseService.ExpenseStatistics statistics = expenseService.getGroupExpenseStatistics(
        groupId, authentication);

    return ResponseEntity.ok(statistics);
  }
}