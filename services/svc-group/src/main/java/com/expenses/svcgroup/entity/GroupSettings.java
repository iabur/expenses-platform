package com.expenses.svcgroup.entity;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "group_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = { "group" })
public class GroupSettings {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @EqualsAndHashCode.Include
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false, unique = true)
  private Group group;

  @Column(name = "simplify_debts", nullable = false)
  @Builder.Default
  private Boolean simplifyDebts = true;

  @Column(name = "auto_settle_threshold", precision = 15, scale = 2)
  @Builder.Default
  private BigDecimal autoSettleThreshold = new BigDecimal("0.01");

  @Column(name = "allow_non_members_to_view", nullable = false)
  @Builder.Default
  private Boolean allowNonMembersToView = false;

  @Column(name = "require_approval_for_expenses", nullable = false)
  @Builder.Default
  private Boolean requireApprovalForExpenses = false;

  @Column(name = "notification_new_expense", nullable = false)
  @Builder.Default
  private Boolean notificationNewExpense = true;

  @Column(name = "notification_expense_update", nullable = false)
  @Builder.Default
  private Boolean notificationExpenseUpdate = true;

  @Column(name = "notification_payment", nullable = false)
  @Builder.Default
  private Boolean notificationPayment = true;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  // Helper methods
  public boolean shouldSimplifyDebts() {
    return Boolean.TRUE.equals(simplifyDebts);
  }

  public boolean shouldAutoSettle(BigDecimal amount) {
    return autoSettleThreshold != null &&
        amount.compareTo(autoSettleThreshold) <= 0;
  }

  public boolean isPubliclyViewable() {
    return Boolean.TRUE.equals(allowNonMembersToView);
  }

  public boolean requiresExpenseApproval() {
    return Boolean.TRUE.equals(requireApprovalForExpenses);
  }

  public void enableNotifications() {
    notificationNewExpense = true;
    notificationExpenseUpdate = true;
    notificationPayment = true;
  }

  public void disableNotifications() {
    notificationNewExpense = false;
    notificationExpenseUpdate = false;
    notificationPayment = false;
  }
}