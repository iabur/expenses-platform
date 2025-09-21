package com.expenses.svcexpense.web;

import java.util.UUID;

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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Expenses", description = "Expense management operations for creating, updating, and tracking shared expenses")
public class ExpenseController {

  private final ExpenseService expenseService;

  @PostMapping
  @Operation(summary = "Create expense", description = "Creates a new expense in a group with specified participants and split method. The expense can include line items, attachments, and custom split configurations.", operationId = "createExpense", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Expense created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseDto.ExpenseResponse.class), examples = @ExampleObject(name = "Created Expense", value = """
          {
            "id": "expense-123",
            "groupId": "550e8400-e29b-41d4-a716-446655440000",
            "title": "Dinner at Restaurant",
            "description": "Group dinner with friends",
            "amountCents": 8500,
            "currency": "USD",
            "category": "FOOD",
            "splitMethod": "EQUAL",
            "createdBy": "123e4567-e89b-12d3-a456-426614174000",
            "createdAt": "2024-01-15T10:30:00Z",
            "participants": [
              {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "amountOwedCents": 2125,
                "isPaid": false
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid expense data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Not authorized to create expense in this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseDto.ExpenseResponse> createExpense(
      @Valid @RequestBody ExpenseDto.CreateExpenseRequest request,
      Authentication authentication) {

    log.info("Creating expense for group {}", request.groupId());

    ExpenseDto.ExpenseResponse response = expenseService.createExpense(request, authentication);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{expenseId}")
  @Operation(summary = "Get expense", description = "Retrieves detailed information about a specific expense including participants, line items, and payment status. Only group members can view expense details.", operationId = "getExpense", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Expense found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseDto.ExpenseResponse.class), examples = @ExampleObject(name = "Expense Details", value = """
          {
            "id": "expense-123",
            "groupId": "550e8400-e29b-41d4-a716-446655440000",
            "title": "Dinner at Restaurant",
            "description": "Group dinner with friends",
            "amountCents": 8500,
            "currency": "USD",
            "category": "FOOD",
            "splitMethod": "EQUAL",
            "status": "ACTIVE",
            "createdBy": "123e4567-e89b-12d3-a456-426614174000",
            "createdAt": "2024-01-15T10:30:00Z",
            "updatedAt": "2024-01-15T10:30:00Z",
            "participants": [
              {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "amountOwedCents": 2125,
                "isPaid": false
              }
            ],
            "lineItems": [
              {
                "description": "Main course",
                "amountCents": 6000
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Not authorized to view this expense", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Expense not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseDto.ExpenseResponse> getExpense(
      @Parameter(description = "Unique identifier of the expense", required = true, example = "expense-123") @PathVariable UUID expenseId,
      Authentication authentication) {

    ExpenseDto.ExpenseResponse response = expenseService.getExpense(expenseId, authentication);

    return ResponseEntity.ok(response);
  }

  @PutMapping("/{expenseId}")
  @Operation(summary = "Update expense", description = "Updates expense details including title, description, amount, participants, and line items. Only the expense creator or group administrators can update expenses.", operationId = "updateExpense", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Expense updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseDto.ExpenseResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid expense data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Not authorized to update this expense", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Expense not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseDto.ExpenseResponse> updateExpense(
      @Parameter(description = "Unique identifier of the expense to update", required = true, example = "expense-123") @PathVariable UUID expenseId,
      @Valid @RequestBody ExpenseDto.UpdateExpenseRequest request,
      Authentication authentication) {

    log.info("Updating expense {}", expenseId);

    ExpenseDto.ExpenseResponse response = expenseService.updateExpense(expenseId, request, authentication);

    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{expenseId}")
  @Operation(summary = "Delete expense", description = "Soft deletes an expense, marking it as deleted but preserving the data for audit purposes. Only the expense creator or group administrators can delete expenses.", operationId = "deleteExpense", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Expense deleted successfully"),
      @ApiResponse(responseCode = "403", description = "Not authorized to delete this expense", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Expense not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteExpense(
      @Parameter(description = "Unique identifier of the expense to delete", required = true, example = "expense-123") @PathVariable UUID expenseId,
      Authentication authentication) {

    log.info("Deleting expense {}", expenseId);

    expenseService.deleteExpense(expenseId, authentication);

    return ResponseEntity.noContent().build();
  }

  @GetMapping("/group/{groupId}")
  @Operation(summary = "Get group expenses", description = "Retrieves a paginated list of all expenses for a specific group, ordered by creation date (newest first). Only group members can view group expenses.", operationId = "getGroupExpenses", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Group expenses retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "403", description = "Not authorized to view expenses for this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getGroupExpenses(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getGroupExpenses(
        groupId, pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/my")
  @Operation(summary = "Get my expenses", description = "Retrieves a paginated list of all expenses created by the authenticated user across all groups.", operationId = "getMyExpenses", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User expenses retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getMyExpenses(
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getUserExpenses(
        pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/participated")
  @Operation(summary = "Get participated expenses", description = "Retrieves a paginated list of all expenses where the authenticated user is a participant (owes money or is owed money).", operationId = "getParticipatedExpenses", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Participated expenses retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> getParticipatedExpenses(
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.getUserParticipatedExpenses(
        pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/search")
  @Operation(summary = "Search expenses", description = "Searches for expenses by title, description, or category. Results can be filtered by group and are returned in a paginated format.", operationId = "searchExpenses", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Search results retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "400", description = "Invalid search parameters", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<ExpenseDto.ExpenseSummary>> searchExpenses(
      @Parameter(description = "Search query to match against expense title, description, or category", required = true, example = "dinner") @RequestParam String query,
      @Parameter(description = "Optional group ID to limit search to specific group", example = "550e8400-e29b-41d4-a716-446655440000") @RequestParam(required = false) UUID groupId,
      @PageableDefault(size = 20) Pageable pageable,
      Authentication authentication) {

    Page<ExpenseDto.ExpenseSummary> expenses = expenseService.searchExpenses(
        query, groupId, pageable, authentication);

    return ResponseEntity.ok(expenses);
  }

  @GetMapping("/group/{groupId}/statistics")
  @Operation(summary = "Get group expense statistics", description = "Retrieves comprehensive expense statistics for a group including total amounts, category breakdowns, and member spending patterns. Only group members can view statistics.", operationId = "getGroupStatistics", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Statistics retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ExpenseService.ExpenseStatistics.class), examples = @ExampleObject(name = "Group Statistics", value = """
          {
            "totalExpenses": 125000,
            "totalExpenseCount": 15,
            "averageExpenseAmount": 8333,
            "categoryBreakdown": {
              "FOOD": 75000,
              "TRANSPORT": 30000,
              "ENTERTAINMENT": 20000
            },
            "memberSpending": [
              {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "totalSpent": 45000,
                "expenseCount": 8
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Not authorized to view statistics for this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<ExpenseService.ExpenseStatistics> getGroupStatistics(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      Authentication authentication) {

    ExpenseService.ExpenseStatistics statistics = expenseService.getGroupExpenseStatistics(
        groupId, authentication);

    return ResponseEntity.ok(statistics);
  }
}