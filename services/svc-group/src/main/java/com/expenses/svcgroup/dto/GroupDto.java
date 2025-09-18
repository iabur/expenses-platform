package com.expenses.svcgroup.dto;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.expenses.svcgroup.entity.Group;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroupDto(
    UUID id,

    @NotBlank(message = "Group name is required") @Size(max = 200, message = "Group name must be less than 200 characters") String name,

    String description,
    Group.GroupType type,

    @Size(min = 3, max = 3, message = "Currency code must be 3 characters") String defaultCurrency,

    String avatarUrl,
    UUID createdBy,
    ZonedDateTime createdAt,
    ZonedDateTime updatedAt,
    Boolean isActive,
    Long memberCount,
    List<GroupMemberDto> members,
    GroupSettingsDto settings) {

  @Builder
  public GroupDto {
  }

  // Factory methods
  public static GroupDto from(Group group) {
    return GroupDto.builder()
        .id(group.getId())
        .name(group.getName())
        .description(group.getDescription())
        .type(group.getType())
        .defaultCurrency(group.getDefaultCurrency())
        .avatarUrl(group.getAvatarUrl())
        .createdBy(group.getCreatedBy())
        .createdAt(group.getCreatedAt())
        .updatedAt(group.getUpdatedAt())
        .isActive(group.getIsActive())
        .memberCount(group.getActiveMemberCount())
        .build();
  }

  public static GroupDto fromWithMembers(Group group) {
    return GroupDto.builder()
        .id(group.getId())
        .name(group.getName())
        .description(group.getDescription())
        .type(group.getType())
        .defaultCurrency(group.getDefaultCurrency())
        .avatarUrl(group.getAvatarUrl())
        .createdBy(group.getCreatedBy())
        .createdAt(group.getCreatedAt())
        .updatedAt(group.getUpdatedAt())
        .isActive(group.getIsActive())
        .memberCount(group.getActiveMemberCount())
        .members(group.getMembers().stream()
            .filter(member -> member.getStatus() == com.expenses.svcgroup.entity.GroupMember.MemberStatus.ACTIVE)
            .map(GroupMemberDto::from)
            .toList())
        .settings(group.getSettings() != null ? GroupSettingsDto.from(group.getSettings()) : null)
        .build();
  }

  // Request DTOs
  public record CreateGroupRequest(
      @NotBlank(message = "Group name is required") @Size(max = 200, message = "Group name must be less than 200 characters") String name,

      String description,
      Group.GroupType type,

      @Size(min = 3, max = 3, message = "Currency code must be 3 characters") String defaultCurrency,

      String avatarUrl) {
  }

  public record UpdateGroupRequest(
      String name,
      String description,
      String defaultCurrency,
      String avatarUrl) {
  }
}
