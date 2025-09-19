package com.expenses.svcledger.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"postings"})
@ToString(exclude = {"postings"})
public class JournalEntry {

    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 20, nullable = false)
    @NotNull(message = "Reference type is required")
    private ReferenceType referenceType;

    @Column(name = "reference_id", nullable = false)
    @NotNull(message = "Reference ID is required")
    private UUID referenceId;

    @Column(name = "group_id", nullable = false)
    @NotNull(message = "Group ID is required")
    private UUID groupId;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    @NotBlank(message = "Description is required")
    private String description;

    @Column(name = "value_date", nullable = false)
    @NotNull(message = "Value date is required")
    private LocalDate valueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    @NotNull(message = "Created by user ID is required")
    private UUID createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Posting> postings = new ArrayList<>();

    // Helper methods
    public void addPosting(Posting posting) {
        if (postings == null) {
            postings = new ArrayList<>();
        }
        postings.add(posting);
        posting.setJournalEntry(this);
    }

    public void removePosting(Posting posting) {
        if (postings != null) {
            postings.remove(posting);
            posting.setJournalEntry(null);
        }
    }

    public long getTotalDebits() {
        if (postings == null)
            return 0L;
        return postings.stream()
                .filter(p -> p.getDebitAccount() != null)
                .mapToLong(Posting::getAmountCents)
                .sum();
    }

    public long getTotalCredits() {
        if (postings == null)
            return 0L;
        return postings.stream()
                .filter(p -> p.getCreditAccount() != null)
                .mapToLong(Posting::getAmountCents)
                .sum();
    }

    public boolean isBalanced() {
        return getTotalDebits() == getTotalCredits();
    }

    public boolean hasPostings() {
        return postings != null && !postings.isEmpty();
    }

    public int getPostingCount() {
        return postings != null ? postings.size() : 0;
    }

    // Business methods
    public String getJournalSummary() {
        return String.format("Journal Entry: %s - %s (Debits: %s, Credits: %s, Balanced: %s)",
                referenceType, description, getTotalDebits(), getTotalCredits(), isBalanced());
    }

    public boolean isExpenseEntry() {
        return referenceType == ReferenceType.EXPENSE;
    }

    public boolean isPaymentEntry() {
        return referenceType == ReferenceType.PAYMENT;
    }

    public boolean isSettlementEntry() {
        return referenceType == ReferenceType.SETTLEMENT;
    }

    // Factory methods
    public static JournalEntry forExpense(UUID expenseId, UUID groupId, String description,
                                          LocalDate valueDate, UUID createdBy) {
        return JournalEntry.builder()
                .referenceType(ReferenceType.EXPENSE)
                .referenceId(expenseId)
                .groupId(groupId)
                .description(description)
                .valueDate(valueDate)
                .createdBy(createdBy)
                .build();
    }

    public static JournalEntry forSettlement(UUID settlementId, UUID groupId, String description,
                                             LocalDate valueDate, UUID createdBy) {
        return JournalEntry.builder()
                .referenceType(ReferenceType.SETTLEMENT)
                .referenceId(settlementId)
                .groupId(groupId)
                .description(description)
                .valueDate(valueDate)
                .createdBy(createdBy)
                .build();
    }

    public static JournalEntry forPayment(UUID paymentId, UUID groupId, String description,
                                          LocalDate valueDate, UUID createdBy) {
        return JournalEntry.builder()
                .referenceType(ReferenceType.PAYMENT)
                .referenceId(paymentId)
                .groupId(groupId)
                .description(description)
                .valueDate(valueDate)
                .createdBy(createdBy)
                .build();
    }

    // Reference type enum
    public enum ReferenceType {
        EXPENSE("Expense Entry"),
        PAYMENT("Payment Entry"),
        SETTLEMENT("Settlement Entry"),
        ADJUSTMENT("Balance Adjustment");

        private final String displayName;

        ReferenceType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}