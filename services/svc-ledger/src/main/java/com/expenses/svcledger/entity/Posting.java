package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "postings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"journalEntry", "debitAccount", "creditAccount"})
@ToString(exclude = {"journalEntry", "debitAccount", "creditAccount"})
public class Posting {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    @NotNull(message = "Journal entry is required")
    private JournalEntry journalEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_account_id")
    private Account debitAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_account_id")
    private Account creditAccount;

    @Column(name = "amount_cents", nullable = false)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Long amountCents;

    @Column(name = "currency", length = 3, nullable = false)
    @NotNull(message = "Currency is required")
    private String currency;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    // Helper methods
    public BigDecimal getAmountDecimal() {
        return amountCents != null ? BigDecimal.valueOf(amountCents, 2) : BigDecimal.ZERO;
    }

    public void setAmountDecimal(BigDecimal amount) {
        this.amountCents = amount != null ? amount.movePointRight(2).longValue() : 0L;
    }

    public boolean isDebit() {
        return debitAccount != null;
    }

    public boolean isCredit() {
        return creditAccount != null;
    }

    public boolean isValid() {
        // A posting must be either debit OR credit, not both or neither
        return (debitAccount != null) ^ (creditAccount != null);
    }

    public String getFormattedAmount() {
        if (amountCents == null)
            return "0.00";
        return String.format("%.2f", amountCents / 100.0);
    }

    public String getAmountWithCurrency() {
        return String.format("%s %s", getFormattedAmount(), currency);
    }

    // Business methods
    public Account getAccount() {
        return debitAccount != null ? debitAccount : creditAccount;
    }

    public String getPostingType() {
        if (isDebit())
            return "DEBIT";
        if (isCredit())
            return "CREDIT";
        return "INVALID";
    }

    public String getPostingSummary() {
        Account account = getAccount();
        String accountName = account != null ? account.getAccountName() : "Unknown";
        return String.format("%s %s: %s", getPostingType(), accountName, getAmountWithCurrency());
    }

    // Factory methods for common posting types
    public static Posting debit(Account account, Long amountCents, String currency, String description) {
        return Posting.builder()
                .debitAccount(account)
                .amountCents(amountCents)
                .currency(currency)
                .description(description)
                .build();
    }

    public static Posting credit(Account account, Long amountCents, String currency, String description) {
        return Posting.builder()
                .creditAccount(account)
                .amountCents(amountCents)
                .currency(currency)
                .description(description)
                .build();
    }

    // Validation methods
    public void validatePosting() {
        if (!isValid()) {
            throw new IllegalStateException("Posting must have either debit or credit account, not both or neither");
        }

        if (amountCents == null || amountCents <= 0) {
            throw new IllegalStateException("Posting amount must be positive");
        }

        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalStateException("Posting currency is required");
        }

        Account account = getAccount();
        if (account != null && !account.getCurrency().equals(currency)) {
            throw new IllegalStateException("Posting currency must match account currency");
        }
    }
}