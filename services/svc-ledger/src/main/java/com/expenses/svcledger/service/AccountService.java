package com.expenses.svcledger.service;

import com.expenses.svcledger.dto.AccountDto;
import com.expenses.svcledger.entity.Account;
import com.expenses.svcledger.entity.AccountBalance;
import com.expenses.svcledger.exception.AccountAlreadyExistsException;
import com.expenses.svcledger.exception.AccountNotFoundException;
import com.expenses.svcledger.repository.AccountBalanceRepository;
import com.expenses.svcledger.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository accountBalanceRepository;

    /**
     * Create account for user or group
     */
    public AccountDto createAccount(AccountDto.CreateAccountRequest request) {
        log.info("Creating account for {} {} with currency {}",
                request.ownerType(), request.ownerId(), request.currency());

        // Check if account already exists
        if (accountRepository.existsByOwnerTypeAndOwnerIdAndCurrency(
                request.ownerType(), request.ownerId(), request.currency())) {
            throw new AccountAlreadyExistsException(
                    String.format("Account already exists for %s %s with currency %s",
                            request.ownerType(), request.ownerId(), request.currency()));
        }

        Account account = request.toEntity();
        Account savedAccount = accountRepository.save(account);

        log.info("Created account: {} - {}", savedAccount.getAccountCode(), savedAccount.getAccountName());

        return AccountDto.from(savedAccount);
    }

    /**
     * Get or create account for user/group and currency
     */
    public AccountDto getOrCreateAccount(Account.OwnerType ownerType, UUID ownerId, String currency, String accountName) {
        log.debug("Getting or creating account for {} {} with currency {}", ownerType, ownerId, currency);

        Optional<Account> existingAccount = accountRepository.findByOwnerTypeAndOwnerIdAndCurrencyAndIsActiveTrue(
                ownerType, ownerId, currency);

        if (existingAccount.isPresent()) {
            log.debug("Found existing account: {}", existingAccount.get().getAccountCode());
            return AccountDto.from(existingAccount.get());
        }

        // Create new account
        AccountDto.CreateAccountRequest request = AccountDto.CreateAccountRequest.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .currency(currency)
                .accountName(accountName != null ? accountName : generateDefaultAccountName(ownerType, currency))
                .build();

        return createAccount(request);
    }

    /**
     * Get account by ID
     */
    @Transactional(readOnly = true)
    public AccountDto getAccountById(UUID accountId) {
        log.debug("Fetching account by ID: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        return AccountDto.from(account);
    }

    /**
     * Get account by account code
     */
    @Transactional(readOnly = true)
    public AccountDto getAccountByCode(String accountCode) {
        log.debug("Fetching account by code: {}", accountCode);

        Account account = accountRepository.findByAccountCode(accountCode)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountCode));

        return AccountDto.from(account);
    }

    /**
     * Get all accounts for a user
     */
    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(UUID userId) {
        log.debug("Fetching accounts for user: {}", userId);

        return accountRepository.findUserAccounts(userId)
                .stream()
                .map(AccountDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get all accounts for a group
     */
    @Transactional(readOnly = true)
    public List<AccountDto> getGroupAccounts(UUID groupId) {
        log.debug("Fetching accounts for group: {}", groupId);

        return accountRepository.findGroupAccounts(groupId)
                .stream()
                .map(AccountDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get user accounts with balances
     */
    @Transactional(readOnly = true)
    public List<AccountDto.AccountWithBalance> getUserAccountsWithBalances(UUID userId) {
        log.debug("Fetching user accounts with balances: {}", userId);

        List<Account> accounts = accountRepository.findUserAccounts(userId);

        return accounts.stream()
                .map(account -> {
                    AccountBalance balance = accountBalanceRepository.findByAccount(account).orElse(null);
                    Long balanceCents = balance != null ? balance.getBalanceCents() : 0L;
                    String formattedBalance = balance != null ? balance.getFormattedBalance() : "0.00";
                    String balanceDescription = balance != null ? balance.getBalanceDescription() : "settled";
                    ZonedDateTime lastUpdated = balance != null ? balance.getLastUpdated() : null;

                    return AccountDto.AccountWithBalance.builder()
                            .account(AccountDto.from(account))
                            .balanceCents(balanceCents)
                            .formattedBalance(formattedBalance + " " + account.getCurrency())
                            .balanceDescription(balanceDescription)
                            .lastUpdated(lastUpdated)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Update account
     */
    public AccountDto updateAccount(UUID accountId, AccountDto.UpdateAccountRequest request) {
        log.info("Updating account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        request.updateEntity(account);
        Account updatedAccount = accountRepository.save(account);

        log.info("Updated account: {} - {}", updatedAccount.getAccountCode(), updatedAccount.getAccountName());

        return AccountDto.from(updatedAccount);
    }

    /**
     * Activate account
     */
    public AccountDto activateAccount(UUID accountId) {
        log.info("Activating account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        account.activate();
        Account activatedAccount = accountRepository.save(account);

        log.info("Activated account: {}", activatedAccount.getAccountCode());

        return AccountDto.from(activatedAccount);
    }

    /**
     * Deactivate account
     */
    public AccountDto deactivateAccount(UUID accountId) {
        log.info("Deactivating account: {}", accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        account.deactivate();
        Account deactivatedAccount = accountRepository.save(account);

        log.info("Deactivated account: {}", deactivatedAccount.getAccountCode());

        return AccountDto.from(deactivatedAccount);
    }

    /**
     * Get accounts with pagination and filtering
     */
    @Transactional(readOnly = true)
    public Page<AccountDto> getAccountsWithFilters(Account.OwnerType ownerType, UUID ownerId,
                                                   String currency, Boolean isActive,
                                                   Pageable pageable) {
        log.debug("Fetching accounts with filters - ownerType: {}, ownerId: {}, currency: {}, isActive: {}",
                ownerType, ownerId, currency, isActive);

        return accountRepository.findAccountsWithFilters(ownerType, ownerId, currency, isActive, pageable)
                .map(AccountDto::from);
    }

    /**
     * Search accounts by name
     */
    @Transactional(readOnly = true)
    public List<AccountDto> searchAccountsByName(String namePattern) {
        log.debug("Searching accounts by name pattern: {}", namePattern);

        return accountRepository.findByAccountNameContaining(namePattern)
                .stream()
                .map(AccountDto::from)
                .collect(Collectors.toList());
    }

    /**
     * Get account statistics
     */
    @Transactional(readOnly = true)
    public AccountStats getAccountStatistics() {
        List<Object[]> stats = accountRepository.getAccountStatistics();

        long totalUserAccounts = accountRepository.countByOwnerTypeAndIsActiveTrue(Account.OwnerType.USER);
        long totalGroupAccounts = accountRepository.countByOwnerTypeAndIsActiveTrue(Account.OwnerType.GROUP);
        long totalSystemAccounts = accountRepository.countByOwnerTypeAndIsActiveTrue(Account.OwnerType.SYSTEM);

        return new AccountStats(totalUserAccounts, totalGroupAccounts, totalSystemAccounts, stats);
    }

    // Helper methods
    private String generateDefaultAccountName(Account.OwnerType ownerType, String currency) {
        return String.format("%s %s Account", currency, ownerType.getDisplayName());
    }

    // Helper record for statistics
    public record AccountStats(
            long totalUserAccounts,
            long totalGroupAccounts,
            long totalSystemAccounts,
            List<Object[]> detailedStats) {
    }
}
