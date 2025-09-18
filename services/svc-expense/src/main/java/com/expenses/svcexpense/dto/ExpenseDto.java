package com.expenses.svcexpense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.expenses.svcexpense.entity.Expense;
import com.expenses.svcexpense.entity.ExpenseAttachment;
import com.expenses.svcexpense.entity.ExpenseLineItem;
import com.expenses.svcexpense.entity.ExpenseParticipant;

import lombok.Builder;

/**
 * Expense DTOs for API communication
 */
public class ExpenseDto {

  /**
   * Complete expense information
   */
  @Builder
  public record ExpenseResponse(
      UUID id,
      UUID groupId,
      UUID creatorId,
      String currency,
      Long amountCents,
      BigDecimal amount,
      LocalDate occurredAt,
      String note,
      String category,
      BigDecimal fxRate,
      String fxBaseCurrency,
      ZonedDateTime createdAt,
      ZonedDateTime updatedAt,
      Boolean isDeleted,
      List<ParticipantResponse> participants,
      List<LineItemResponse> lineItems,
      List<AttachmentResponse> attachments) {

    public static ExpenseResponse from(Expense expense) {
      return ExpenseResponse.builder()
          .id(expense.getId())
          .groupId(expense.getGroupId())
          .creatorId(expense.getCreatorId())
          .currency(expense.getCurrency())
          .amountCents(expense.getAmountCents())
          .amount(expense.getAmountDecimal())
          .occurredAt(expense.getOccurredAt())
          .note(expense.getNote())
          .category(expense.getCategory())
          .fxRate(expense.getFxRate())
          .fxBaseCurrency(expense.getFxBaseCurrency())
          .createdAt(expense.getCreatedAt())
          .updatedAt(expense.getUpdatedAt())
          .isDeleted(expense.getIsDeleted())
          .participants(expense.getParticipants().stream()
              .map(ParticipantResponse::from)
              .toList())
          .lineItems(expense.getLineItems().stream()
              .map(LineItemResponse::from)
              .toList())
          .attachments(expense.getAttachments().stream()
              .map(AttachmentResponse::from)
              .toList())
          .build();
    }
  }

  /**
   * Create expense request
   */
  @Builder
  public record CreateExpenseRequest(
      @NotNull(message = "Group ID is required") UUID groupId,

      @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be 3 characters") String currency,

      @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,

      @NotNull(message = "Occurred date is required") @PastOrPresent(message = "Expense date cannot be in the future") LocalDate occurredAt,

      @Size(max = 1000, message = "Note cannot exceed 1000 characters") String note,

      @NotBlank(message = "Category is required") @Size(max = 100, message = "Category cannot exceed 100 characters") String category,

      @NotEmpty(message = "At least one participant is required") @Valid List<ParticipantRequest> participants,

      @Valid List<LineItemRequest> lineItems) {
  }

  /**
   * Update expense request
   */
  @Builder
  public record UpdateExpenseRequest(
      @NotBlank(message = "Currency is required") @Size(min = 3, max = 3, message = "Currency must be 3 characters") String currency,

      @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount,

      @NotNull(message = "Occurred date is required") @PastOrPresent(message = "Expense date cannot be in the future") LocalDate occurredAt,

      @Size(max = 1000, message = "Note cannot exceed 1000 characters") String note,

      @NotBlank(message = "Category is required") @Size(max = 100, message = "Category cannot exceed 100 characters") String category,

      @Valid List<ParticipantRequest> participants,

      @Valid List<LineItemRequest> lineItems) {
  }

  /**
   * Participant in expense
   */
  @Builder
  public record ParticipantResponse(
      UUID id,
      UUID userId,
      String ruleType,
      BigDecimal ruleValue,
      Long calculatedAmountCents,
      BigDecimal calculatedAmount,
      ZonedDateTime createdAt) {

    public static ParticipantResponse from(ExpenseParticipant participant) {
      return ParticipantResponse.builder()
          .id(participant.getId())
          .userId(participant.getUserId())
          .ruleType(participant.getRuleType().name())
          .ruleValue(participant.getRuleValue())
          .calculatedAmountCents(participant.getCalculatedAmountCents())
          .calculatedAmount(participant.getCalculatedAmountDecimal())
          .createdAt(participant.getCreatedAt())
          .build();
    }
  }

  /**
   * Participant request for expense
   */
  @Builder
  public record ParticipantRequest(
      @NotNull(message = "User ID is required") UUID userId,

      @NotNull(message = "Rule type is required") ExpenseParticipant.SplitRuleType ruleType,

      @DecimalMin(value = "0.0", message = "Rule value must be non-negative") BigDecimal ruleValue) {
  }

  /**
   * Line item response
   */
  @Builder
  public record LineItemResponse(
      UUID id,
      String description,
      Integer quantity,
      Long unitPriceCents,
      BigDecimal unitPrice,
      Long totalPriceCents,
      BigDecimal totalPrice,
      String category,
      ZonedDateTime createdAt) {

    public static LineItemResponse from(ExpenseLineItem lineItem) {
      return LineItemResponse.builder()
          .id(lineItem.getId())
          .description(lineItem.getDescription())
          .quantity(lineItem.getQuantity())
          .unitPriceCents(lineItem.getUnitPriceCents())
          .unitPrice(lineItem.getUnitPriceDecimal())
          .totalPriceCents(lineItem.getTotalPriceCents())
          .totalPrice(lineItem.getTotalPriceDecimal())
          .category(lineItem.getCategory())
          .createdAt(lineItem.getCreatedAt())
          .build();
    }
  }

  /**
   * Line item request
   */
  @Builder
  public record LineItemRequest(
      @NotBlank(message = "Description is required") @Size(max = 200, message = "Description cannot exceed 200 characters") String description,

      @NotNull(message = "Quantity is required") @Positive(message = "Quantity must be positive") Integer quantity,

      @NotNull(message = "Unit price is required") @DecimalMin(value = "0.01", message = "Unit price must be positive") BigDecimal unitPrice,

      @Size(max = 100, message = "Category cannot exceed 100 characters") String category) {
  }

  /**
   * Attachment response
   */
  @Builder
  public record AttachmentResponse(
      UUID id,
      String fileName,
      String fileUrl,
      Long fileSizeBytes,
      String mimeType,
      UUID uploadedBy,
      ZonedDateTime createdAt) {

    public static AttachmentResponse from(ExpenseAttachment attachment) {
      return AttachmentResponse.builder()
          .id(attachment.getId())
          .fileName(attachment.getFileName())
          .fileUrl(attachment.getFileUrl())
          .fileSizeBytes(attachment.getFileSizeBytes())
          .mimeType(attachment.getMimeType())
          .uploadedBy(attachment.getUploadedBy())
          .createdAt(attachment.getCreatedAt())
          .build();
    }
  }

  /**
   * Expense summary for lists
   */
  @Builder
  public record ExpenseSummary(
      UUID id,
      UUID groupId,
      UUID creatorId,
      String currency,
      BigDecimal amount,
      LocalDate occurredAt,
      String note,
      String category,
      Integer participantCount,
      ZonedDateTime createdAt) {

    public static ExpenseSummary from(Expense expense) {
      return ExpenseSummary.builder()
          .id(expense.getId())
          .groupId(expense.getGroupId())
          .creatorId(expense.getCreatorId())
          .currency(expense.getCurrency())
          .amount(expense.getAmountDecimal())
          .occurredAt(expense.getOccurredAt())
          .note(expense.getNote())
          .category(expense.getCategory())
          .participantCount(expense.getParticipants().size())
          .createdAt(expense.getCreatedAt())
          .build();
    }
  }
}
