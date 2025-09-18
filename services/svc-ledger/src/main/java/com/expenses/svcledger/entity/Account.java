package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "owner_type", "owner_id", "currency" }),
    @UniqueConstraint(columnNames = { "account_code" })
})
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "account_code", unique = true, nullable = false)
  @NotBlank(message = "Account code is required")
  private String accountCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", length = 20, nullable = false)
  private OwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  @NotNull(message = "Owner ID is required")
  private UUID ownerId;

  @Column(length = 3, nullable = false)
  @NotBlank(message = "Currency is required")
  private String currency;

  @Column(name = "account_name", nullable = false)
  @NotBlank(message = "Account name is required")
  private String accountName;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @OneToMany(mappedBy = "debitAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Posting> debitPostings = new HashSet<>();

  @OneToMany(mappedBy = "creditAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Posting> creditPostings = new HashSet<>();

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private AccountBalance balance;

  // Constructors
  public Account() {
  }

  public Account(String accountCode, OwnerType ownerType, UUID ownerId, String currency, String accountName) {
    this.accountCode = accountCode;
    this.ownerType = ownerType;
    this.ownerId = ownerId;
    this.currency = currency;
    this.accountName = accountName;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public void setAccountCode(String accountCode) {
    this.accountCode = accountCode;
  }

  public OwnerType getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(OwnerType ownerType) {
    this.ownerType = ownerType;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Set<Posting> getDebitPostings() {
    return debitPostings;
  }

  public void setDebitPostings(Set<Posting> debitPostings) {
    this.debitPostings = debitPostings;
  }

  public Set<Posting> getCreditPostings() {
    return creditPostings;
  }

  public void setCreditPostings(Set<Posting> creditPostings) {
    this.creditPostings = creditPostings;
  }

  public AccountBalance getBalance() {
    return balance;
  }

  public void setBalance(AccountBalance balance) {
    this.balance = balance;
  }

  // Helper methods
  public static String generateAccountCode(OwnerType ownerType, UUID ownerId, String currency) {
    return ownerType.name().toLowerCase() + ":" + ownerId + ":" + currency;
  }

  public boolean isUserAccount() {
    return ownerType == OwnerType.USER;
  }

  public boolean isGroupAccount() {
    return ownerType == OwnerType.GROUP;
  }

  // Owner type enum
  public enum OwnerType {
    USER, GROUP
  }
}
