package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "owner_type", "owner_id", "currency" }),
    @UniqueConstraint(columnNames = { "account_code" })
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = { "debitPostings", "creditPostings", "balance" })
@ToString(exclude = { "debitPostings", "creditPostings", "balance" })
public class Account {

  @Id
  @GeneratedValue
  @Column(name = "id")
  private UUID id;

  @Column(name = "account_code", unique = true, nullable = false, length = 100)
  @NotBlank(message = "Account code is required")
  private String accountCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", length = 20, nullable = false)
  @NotNull(message = "Owner type is required")
  private OwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  @NotNull(message = "Owner ID is required")
  private UUID ownerId;

  @Column(name = "currency", length = 3, nullable = false)
  @NotBlank(message = "Currency is required")
  private String currency;

  @Column(name = "account_name", nullable = false, length = 255)
  @NotBlank(message = "Account name is required")
  private String accountName;

  @Column(name = "description")
  private String description;

  @Builder.Default
  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Builder.Default
  @OneToMany(mappedBy = "debitAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Posting> debitPostings = new HashSet<>();

  @Builder.Default
  @OneToMany(mappedBy = "creditAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<Posting> creditPostings = new HashSet<>();

  @OneToOne(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private AccountBalance balance;

  // Helper methods
  public static String generateAccountCode(OwnerType ownerType, UUID ownerId, String currency) {
    return String.format("%s:%s:%s", ownerType.name().toLowerCase(), ownerId, currency.toUpperCase());
  }

  public boolean isUserAccount() {
    return ownerType == OwnerType.USER;
  }

  public boolean isGroupAccount() {
    return ownerType == OwnerType.GROUP;
  }

  public boolean isActive() {
    return Boolean.TRUE.equals(isActive);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  // Business methods
  public String getDisplayName() {
    return String.format("%s (%s %s)", accountName, currency, ownerType.name());
  }

  public boolean canDebit() {
    return isActive();
  }

  public boolean canCredit() {
    return isActive();
  }

  // Owner type enum
  public enum OwnerType {
    USER("User Account"),
    GROUP("Group Account"),
    SYSTEM("System Account");

    private final String displayName;

    OwnerType(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
