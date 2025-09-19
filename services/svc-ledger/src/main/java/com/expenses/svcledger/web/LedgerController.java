package com.expenses.svcledger.web;

import com.expenses.svcledger.dto.AccountDto;
import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ledger Service", description = "Double-entry bookkeeping and account management")
public class LedgerController {

  private final AccountService accountService;

  // Account endpoints
  @GetMapping("/accounts")
  @Operation(summary = "Get accounts", description = "Get accounts with optional filtering")
  @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully")
  public ResponseEntity<Page<AccountDto>> getAccounts(
      @Parameter(description = "Owner type") @RequestParam(required = false) Account.OwnerType ownerType,
      @Parameter(description = "Owner ID") @RequestParam(required = false) UUID ownerId,
      @Parameter(description = "Currency") @RequestParam(required = false) String currency,
      @Parameter(description = "Active status") @RequestParam(required = false) Boolean isActive,
      @PageableDefault(size = 20) Pageable pageable) {

    Page<AccountDto> accounts = accountService.getAccountsWithFilters(ownerType, ownerId, currency, isActive, pageable);
    return ResponseEntity.ok(accounts);
  }

  @GetMapping("/accounts/{accountId}")
  @Operation(summary = "Get account by ID", description = "Retrieve account details by ID")
  @ApiResponse(responseCode = "200", description = "Account retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Account not found")
  public ResponseEntity<AccountDto> getAccount(
      @Parameter(description = "Account ID") @PathVariable UUID accountId) {

    AccountDto account = accountService.getAccountById(accountId);
    return ResponseEntity.ok(account);
  }

  @GetMapping("/accounts/code/{accountCode}")
  @Operation(summary = "Get account by code", description = "Retrieve account details by account code")
  @ApiResponse(responseCode = "200", description = "Account retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Account not found")
  public ResponseEntity<AccountDto> getAccountByCode(
      @Parameter(description = "Account code") @PathVariable String accountCode) {

    AccountDto account = accountService.getAccountByCode(accountCode);
    return ResponseEntity.ok(account);
  }

  @GetMapping("/accounts/user/{userId}")
  @Operation(summary = "Get user accounts", description = "Get all accounts for a specific user")
  @ApiResponse(responseCode = "200", description = "User accounts retrieved successfully")
  public ResponseEntity<List<AccountDto>> getUserAccounts(
      @Parameter(description = "User ID") @PathVariable UUID userId) {

    List<AccountDto> accounts = accountService.getUserAccounts(userId);
    return ResponseEntity.ok(accounts);
  }

  @GetMapping("/accounts/user/{userId}/with-balances")
  @Operation(summary = "Get user accounts with balances", description = "Get all accounts for a user with their current balances")
  @ApiResponse(responseCode = "200", description = "User accounts with balances retrieved successfully")
  public ResponseEntity<List<AccountDto.AccountWithBalance>> getUserAccountsWithBalances(
      @Parameter(description = "User ID") @PathVariable UUID userId) {

    List<AccountDto.AccountWithBalance> accounts = accountService.getUserAccountsWithBalances(userId);
    return ResponseEntity.ok(accounts);
  }

  @GetMapping("/accounts/group/{groupId}")
  @Operation(summary = "Get group accounts", description = "Get all accounts for a specific group")
  @ApiResponse(responseCode = "200", description = "Group accounts retrieved successfully")
  public ResponseEntity<List<AccountDto>> getGroupAccounts(
      @Parameter(description = "Group ID") @PathVariable UUID groupId) {

    List<AccountDto> accounts = accountService.getGroupAccounts(groupId);
    return ResponseEntity.ok(accounts);
  }

  @PostMapping("/accounts")
  @Operation(summary = "Create account", description = "Create a new account for user or group")
  @ApiResponse(responseCode = "200", description = "Account created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid request data")
  @ApiResponse(responseCode = "409", description = "Account already exists")
  public ResponseEntity<AccountDto> createAccount(
      @Valid @RequestBody AccountDto.CreateAccountRequest request,
      Authentication authentication) {

    log.info("Creating account for {} {} by user: {}",
        request.ownerType(), request.ownerId(), authentication.getName());

    AccountDto account = accountService.createAccount(request);
    return ResponseEntity.ok(account);
  }

  @PutMapping("/accounts/{accountId}")
  @Operation(summary = "Update account", description = "Update account information")
  @ApiResponse(responseCode = "200", description = "Account updated successfully")
  @ApiResponse(responseCode = "404", description = "Account not found")
  public ResponseEntity<AccountDto> updateAccount(
      @Parameter(description = "Account ID") @PathVariable UUID accountId,
      @Valid @RequestBody AccountDto.UpdateAccountRequest request,
      Authentication authentication) {

    log.info("Updating account {} by user: {}", accountId, authentication.getName());

    AccountDto account = accountService.updateAccount(accountId, request);
    return ResponseEntity.ok(account);
  }

  @PostMapping("/accounts/{accountId}/activate")
  @Operation(summary = "Activate account", description = "Activate an account")
  @ApiResponse(responseCode = "200", description = "Account activated successfully")
  @ApiResponse(responseCode = "404", description = "Account not found")
  public ResponseEntity<AccountDto> activateAccount(
      @Parameter(description = "Account ID") @PathVariable UUID accountId,
      Authentication authentication) {

    log.info("Activating account {} by user: {}", accountId, authentication.getName());

    AccountDto account = accountService.activateAccount(accountId);
    return ResponseEntity.ok(account);
  }

  @PostMapping("/accounts/{accountId}/deactivate")
  @Operation(summary = "Deactivate account", description = "Deactivate an account")
  @ApiResponse(responseCode = "200", description = "Account deactivated successfully")
  @ApiResponse(responseCode = "404", description = "Account not found")
  public ResponseEntity<AccountDto> deactivateAccount(
      @Parameter(description = "Account ID") @PathVariable UUID accountId,
      Authentication authentication) {

    log.info("Deactivating account {} by user: {}", accountId, authentication.getName());

    AccountDto account = accountService.deactivateAccount(accountId);
    return ResponseEntity.ok(account);
  }

  @GetMapping("/accounts/search")
  @Operation(summary = "Search accounts", description = "Search accounts by name pattern")
  @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
  public ResponseEntity<List<AccountDto>> searchAccounts(
      @Parameter(description = "Name pattern") @RequestParam String namePattern) {

    List<AccountDto> accounts = accountService.searchAccountsByName(namePattern);
    return ResponseEntity.ok(accounts);
  }

  @GetMapping("/accounts/stats")
  @Operation(summary = "Get account statistics", description = "Get account statistics and summary")
  @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
  public ResponseEntity<AccountService.AccountStats> getAccountStatistics() {

    AccountService.AccountStats stats = accountService.getAccountStatistics();
    return ResponseEntity.ok(stats);
  }
}
