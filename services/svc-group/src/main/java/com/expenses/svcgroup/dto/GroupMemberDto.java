package com.expenses.svcgroup.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcgroup.entity.GroupMember;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupMemberDto(
    UUID id,
    UUID groupId,
    UUID userId,
    String userEmail,
    String userDisplayName,
    String userAvatarUrl,
    GroupMember.MemberRole role,
    ZonedDateTime joinedAt,
    GroupMember.MemberStatus status,
    UUID invitedBy,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt) {

  @Builder
  public GroupMemberDto {
  }

  // Factory method
  public static GroupMemberDto from(GroupMember member) {
    return GroupMemberDto.builder()
        .id(member.getId())
        .groupId(member.getGroup().getId())
        .userId(member.getUserId())
        .role(member.getRole())
        .joinedAt(member.getJoinedAt())
        .status(member.getStatus())
        .invitedBy(member.getInvitedBy())
        .createdAt(member.getCreatedAt())
        .updatedAt(member.getUpdatedAt())
        .build();
  }

  // Request DTOs
  public record AddMemberRequest(
      @NotNull(message = "User ID is required") UUID userId,

      GroupMember.MemberRole role) {
  }

  public record InviteMemberRequest(
      @Email(message = "Valid email is required") @NotNull(message = "Email is required") String email,

      GroupMember.MemberRole role) {
  }

  public record UpdateMemberRoleRequest(
      @NotNull(message = "Role is required") GroupMember.MemberRole role) {
  }
}
