package com.expenses.svcgroup.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcgroup.dto.GroupDto;
import com.expenses.svcgroup.entity.Group;
import com.expenses.svcgroup.entity.GroupMember;
import com.expenses.svcgroup.entity.GroupSettings;
import com.expenses.svcgroup.exception.AccessDeniedException;
import com.expenses.svcgroup.exception.GroupNotFoundException;
import com.expenses.svcgroup.exception.UserAlreadyMemberException;
import com.expenses.svcgroup.repository.GroupRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GroupService {

  private final GroupRepository groupRepository;

  /**
   * Create a new group
   */
  public GroupDto createGroup(GroupDto.CreateGroupRequest request, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    log.info("Creating new group '{}' by user {}", request.name(), currentUserId);

    Group group = Group.builder()
        .name(request.name())
        .description(request.description())
        .type(request.type() != null ? request.type() : Group.GroupType.GENERAL)
        .defaultCurrency(request.defaultCurrency() != null ? request.defaultCurrency() : "USD")
        .avatarUrl(request.avatarUrl())
        .createdBy(currentUserId)
        .build();

    // Create default settings
    GroupSettings settings = GroupSettings.builder()
        .group(group)
        .build();
    group.setSettings(settings);

    // Add creator as owner
    GroupMember ownerMember = GroupMember.builder()
        .group(group)
        .userId(currentUserId)
        .role(GroupMember.MemberRole.OWNER)
        .build();
    group.addMember(ownerMember);

    Group savedGroup = groupRepository.save(group);

    log.info("Created group {} with ID {}", savedGroup.getName(), savedGroup.getId());

    return GroupDto.fromWithMembers(savedGroup);
  }

  /**
   * Get group by ID with permission check
   */
  @Transactional(readOnly = true)
  public GroupDto getGroup(UUID groupId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if user has access to view this group
    if (!hasViewAccess(group, currentUserId)) {
      throw new AccessDeniedException("You don't have access to view this group");
    }

    return GroupDto.fromWithMembers(group);
  }

  /**
   * Update group information
   */
  public GroupDto updateGroup(UUID groupId, GroupDto.UpdateGroupRequest request, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if user is admin of the group
    if (!group.hasAdmin(currentUserId)) {
      throw new AccessDeniedException("Only group admins can update group information");
    }

    // Update fields if provided
    if (request.name() != null && !request.name().trim().isEmpty()) {
      group.setName(request.name().trim());
    }
    if (request.description() != null) {
      group.setDescription(request.description().trim().isEmpty() ? null : request.description().trim());
    }
    if (request.defaultCurrency() != null && !request.defaultCurrency().trim().isEmpty()) {
      group.setDefaultCurrency(request.defaultCurrency().trim().toUpperCase());
    }
    if (request.avatarUrl() != null) {
      group.setAvatarUrl(request.avatarUrl().trim().isEmpty() ? null : request.avatarUrl().trim());
    }

    Group savedGroup = groupRepository.save(group);

    log.info("Updated group {} by user {}", groupId, currentUserId);

    return GroupDto.fromWithMembers(savedGroup);
  }

  /**
   * Soft delete group (only owner can delete)
   */
  public void deleteGroup(UUID groupId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if user is owner of the group
    if (!group.hasOwner(currentUserId)) {
      throw new AccessDeniedException("Only group owner can delete the group");
    }

    group.setIsActive(false);
    groupRepository.save(group);

    log.info("Deleted group {} by owner {}", groupId, currentUserId);
  }

  /**
   * Get groups where user is a member
   */
  @Transactional(readOnly = true)
  public Page<GroupDto> getUserGroups(Authentication authentication, Pageable pageable) {
    UUID currentUserId = getCurrentUserId(authentication);

    return groupRepository.findGroupsByUserId(currentUserId, pageable)
        .map(GroupDto::from);
  }

  /**
   * Search groups (only public groups or groups where user is member)
   */
  @Transactional(readOnly = true)
  public Page<GroupDto> searchGroups(String query, Authentication authentication, Pageable pageable) {
    UUID currentUserId = getCurrentUserId(authentication);

    if (query == null || query.trim().isEmpty()) {
      return groupRepository.findAllActiveGroups(pageable)
          .map(group -> hasViewAccess(group, currentUserId) ? GroupDto.from(group) : null)
          .map(dto -> dto); // Filter out nulls
    }

    return groupRepository.searchActiveGroups(query.trim(), pageable)
        .map(group -> hasViewAccess(group, currentUserId) ? GroupDto.from(group) : null)
        .map(dto -> dto); // Filter out nulls
  }

  /**
   * Check if user has admin privileges in group
   */
  @Transactional(readOnly = true)
  public boolean isUserAdminOfGroup(UUID groupId, UUID userId) {
    return groupRepository.isUserAdminOfGroup(groupId, userId);
  }

  /**
   * Check if user is member of group
   */
  @Transactional(readOnly = true)
  public boolean isUserMemberOfGroup(UUID groupId, UUID userId) {
    return groupRepository.isUserMemberOfGroup(groupId, userId);
  }

  /**
   * Get groups where user is admin
   */
  @Transactional(readOnly = true)
  public List<GroupDto> getGroupsWhereUserIsAdmin(Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    return groupRepository.findGroupsWhereUserIsAdmin(currentUserId)
        .stream()
        .map(GroupDto::from)
        .toList();
  }

  // Helper methods
  private UUID getCurrentUserId(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return UUID.fromString(jwt.getSubject());
  }

  private boolean hasViewAccess(Group group, UUID userId) {
    // User can view if:
    // 1. They are a member of the group
    // 2. Group allows non-members to view (public group)
    return group.hasMember(userId) ||
        (group.getSettings() != null && group.getSettings().isPubliclyViewable());
  }
}
