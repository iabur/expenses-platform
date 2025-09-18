package com.expenses.svcgroup.dto;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcgroup.entity.GroupSettings;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupSettingsDto(
    UUID id,
    UUID groupId,
    Boolean simplifyDebts,
    BigDecimal autoSettleThreshold,
    Boolean allowNonMembersToView,
    Boolean requireApprovalForExpenses,
    Boolean notificationNewExpense,
    Boolean notificationExpenseUpdate,
    Boolean notificationPayment,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt) {

  @Builder
  public GroupSettingsDto {
  }

  // Factory method
  public static GroupSettingsDto from(GroupSettings settings) {
    return GroupSettingsDto.builder()
        .id(settings.getId())
        .groupId(settings.getGroup().getId())
        .simplifyDebts(settings.getSimplifyDebts())
        .autoSettleThreshold(settings.getAutoSettleThreshold())
        .allowNonMembersToView(settings.getAllowNonMembersToView())
        .requireApprovalForExpenses(settings.getRequireApprovalForExpenses())
        .notificationNewExpense(settings.getNotificationNewExpense())
        .notificationExpenseUpdate(settings.getNotificationExpenseUpdate())
        .notificationPayment(settings.getNotificationPayment())
        .createdAt(settings.getCreatedAt())
        .updatedAt(settings.getUpdatedAt())
        .build();
  }

  // Request DTO
  public record UpdateSettingsRequest(
      Boolean simplifyDebts,
      BigDecimal autoSettleThreshold,
      Boolean allowNonMembersToView,
      Boolean requireApprovalForExpenses,
      Boolean notificationNewExpense,
      Boolean notificationExpenseUpdate,
      Boolean notificationPayment) {
  }
}
