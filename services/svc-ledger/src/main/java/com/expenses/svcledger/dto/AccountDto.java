package com.expenses.svcledger.dto;

import com.expenses.svcledger.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.UUID;

@Schema(description = "Account information")
public record AccountDto(
    @Schema(description = "Account ID") UUID id,

    @Schema(description = "Account code", example = "user:uuid:USD") String accountCode,

    @Schema(description = "Owner type", example = "USER") Account.OwnerType ownerType,

    @Schema(description = "Owner ID") UUID ownerId,

    @Schema(description = "Currency code", example = "USD") String currency,

    @Schema(description = "Account name", example = "John's USD Account") String accountName,

    @Schema(description = "Account description") String description,

    @Schema(description = "Whether account is active", example = "true") Boolean isActive,

    @Schema(description = "Creation timestamp") ZonedDateTime createdAt,

    @Schema(description = "Last update timestamp") ZonedDateTime updatedAt) {

  // Factory method to create from entity
  public static AccountDto from(Account account) {
    return new AccountDto(
        account.getId(),
        account.getAccountCode(),
        account.getOwnerType(),
        account.getOwnerId(),
        account.getCurrency(),
        account.getAccountName(),
        account.getDescription(),
        account.getIsActive(),
        account.getCreatedAt(),
        account.getUpdatedAt());
  }

  // Request DTOs
  @Schema(description = "Request to create a new account")
  @Builder
  public record CreateAccountRequest(
      @Schema(description = "Owner type", example = "USER", required = true) @NotNull(message = "Owner type is required") Account.OwnerType ownerType,

      @Schema(description = "Owner ID", required = true) @NotNull(message = "Owner ID is required") UUID ownerId,

      @Schema(description = "Currency code", example = "USD", required = true) @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters") String currency,

      @Schema(description = "Account name", example = "John's USD Account", required = true) @NotBlank(message = "Account name is required") String accountName,

      @Schema(description = "Account description") String description) {
    public Account toEntity() {
      String accountCode = Account.generateAccountCode(ownerType, ownerId, currency);

      return Account.builder()
          .accountCode(accountCode)
          .ownerType(ownerType)
          .ownerId(ownerId)
          .currency(currency.toUpperCase())
          .accountName(accountName)
          .description(description)
          .isActive(true)
          .build();
    }
  }

  @Schema(description = "Request to update an account")
  @Builder
  public record UpdateAccountRequest(
      @Schema(description = "Account name", example = "John's Updated Account") String accountName,

      @Schema(description = "Account description") String description,

      @Schema(description = "Whether account is active", example = "true") Boolean isActive) {
    public void updateEntity(Account account) {
      if (accountName != null)
        account.setAccountName(accountName);
      if (description != null)
        account.setDescription(description);
      if (isActive != null)
        account.setIsActive(isActive);
    }
  }

  // Response DTOs
  @Schema(description = "Account summary for lists")
  @Builder
  public record AccountSummary(
      @Schema(description = "Account ID") UUID id,

      @Schema(description = "Account code") String accountCode,

      @Schema(description = "Account name") String accountName,

      @Schema(description = "Currency") String currency,

      @Schema(description = "Owner type") Account.OwnerType ownerType,

      @Schema(description = "Current balance in cents") Long balanceCents,

      @Schema(description = "Formatted balance") String formattedBalance) {
    public static AccountSummary from(Account account, Long balanceCents) {
      String formattedBalance = balanceCents != null
          ? String.format("%.2f %s", balanceCents / 100.0, account.getCurrency())
          : "0.00 " + account.getCurrency();

      return new AccountSummary(
          account.getId(),
          account.getAccountCode(),
          account.getAccountName(),
          account.getCurrency(),
          account.getOwnerType(),
          balanceCents,
          formattedBalance);
    }
  }

  @Schema(description = "Account with balance information")
  @Builder
  public record AccountWithBalance(
      @Schema(description = "Account information") AccountDto account,

      @Schema(description = "Current balance in cents") Long balanceCents,

      @Schema(description = "Formatted balance") String formattedBalance,

      @Schema(description = "Balance description") String balanceDescription,

      @Schema(description = "Last balance update") ZonedDateTime lastUpdated) {
  }
}
