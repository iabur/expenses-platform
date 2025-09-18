package com.expenses.svcgroup.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcgroup.entity.GroupInvitation;
import com.expenses.svcgroup.entity.GroupMember;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupInvitationDto(
    UUID id,
    UUID groupId,
    String groupName,
    UUID invitedBy,
    String invitedByName,
    String invitedEmail,
    UUID invitedUserId,
    GroupMember.MemberRole role,
    String invitationCode,
    ZonedDateTime expiresAt,
    GroupInvitation.InvitationStatus status,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt,
    String invitationUrl,
    Boolean isExpired,
    Boolean canBeAccepted) {

  @Builder
  public GroupInvitationDto {
  }

  // Factory method
  public static GroupInvitationDto from(GroupInvitation invitation, String baseUrl) {
    return GroupInvitationDto.builder()
        .id(invitation.getId())
        .groupId(invitation.getGroup().getId())
        .groupName(invitation.getGroup().getName())
        .invitedBy(invitation.getInvitedBy())
        .invitedEmail(invitation.getInvitedEmail())
        .invitedUserId(invitation.getInvitedUserId())
        .role(invitation.getRole())
        .invitationCode(invitation.getInvitationCode())
        .expiresAt(invitation.getExpiresAt())
        .status(invitation.getStatus())
        .createdAt(invitation.getCreatedAt())
        .updatedAt(invitation.getUpdatedAt())
        .invitationUrl(invitation.getInvitationUrl(baseUrl))
        .isExpired(invitation.isExpired())
        .canBeAccepted(invitation.canBeAccepted())
        .build();
  }

  public static GroupInvitationDto from(GroupInvitation invitation) {
    return from(invitation, "");
  }

  // Request DTOs
  public record CreateInvitationRequest(
      @Email(message = "Valid email is required") @NotBlank(message = "Email is required") String invitedEmail,

      GroupMember.MemberRole role,
      Integer expiryDays // Optional, defaults to 7 days
  ) {
  }

  public record AcceptInvitationRequest(
      @NotBlank(message = "Invitation code is required") String invitationCode) {
  }

  public record ResendInvitationRequest(
      UUID invitationId,
      Integer expiryDays // Optional, defaults to 7 days
  ) {
  }
}
