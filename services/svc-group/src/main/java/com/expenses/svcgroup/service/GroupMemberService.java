package com.expenses.svcgroup.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcgroup.dto.GroupMemberDto;
import com.expenses.svcgroup.entity.Group;
import com.expenses.svcgroup.entity.GroupMember;
import com.expenses.svcgroup.exception.AccessDeniedException;
import com.expenses.svcgroup.exception.GroupNotFoundException;
import com.expenses.svcgroup.exception.MemberNotFoundException;
import com.expenses.svcgroup.exception.UserAlreadyMemberException;
import com.expenses.svcgroup.repository.GroupMemberRepository;
import com.expenses.svcgroup.repository.GroupRepository;
import com.expenses.common.event.EventPublisher;
import com.expenses.common.event.GroupEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class GroupMemberService {

  private final GroupRepository groupRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final EventPublisher eventPublisher;

  /**
   * Add member to group (admin only)
   */
  public GroupMemberDto addMember(UUID groupId, GroupMemberDto.AddMemberRequest request,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if current user is admin
    if (!group.hasAdmin(currentUserId)) {
      throw new AccessDeniedException("Only group admins can add members");
    }

    // Check if user is already a member
    if (groupMemberRepository.existsByGroupIdAndUserIdAndStatus(
        groupId, request.userId(), GroupMember.MemberStatus.ACTIVE)) {
      throw new UserAlreadyMemberException("User is already a member of this group");
    }

    GroupMember member = GroupMember.builder()
        .group(group)
        .userId(request.userId())
        .role(request.role() != null ? request.role() : GroupMember.MemberRole.MEMBER)
        .invitedBy(currentUserId)
        .build();

    GroupMember savedMember = groupMemberRepository.save(member);

    // Publish member added event
    GroupEvent.MemberAdded event = new GroupEvent.MemberAdded(
        groupId,
        request.userId(),
        savedMember.getRole().name(),
        currentUserId);
    eventPublisher.publishEventAsync(event);

    log.info("Added user {} to group {} by admin {}", request.userId(), groupId, currentUserId);

    return GroupMemberDto.from(savedMember);
  }

  /**
   * Get all active members of a group
   */
  @Transactional(readOnly = true)
  public List<GroupMemberDto> getGroupMembers(UUID groupId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if user has access to view members
    if (!hasViewAccess(group, currentUserId)) {
      throw new AccessDeniedException("You don't have access to view group members");
    }

    return groupMemberRepository.findActiveByGroupId(groupId)
        .stream()
        .map(GroupMemberDto::from)
        .toList();
  }

  /**
   * Update member role (admin only)
   */
  public GroupMemberDto updateMemberRole(UUID groupId, UUID memberId,
      GroupMemberDto.UpdateMemberRoleRequest request,
      Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if current user is admin
    if (!group.hasAdmin(currentUserId)) {
      throw new AccessDeniedException("Only group admins can update member roles");
    }

    GroupMember member = groupMemberRepository.findById(memberId)
        .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));

    // Can't change owner role or demote yourself if you're the only owner
    if (member.getRole() == GroupMember.MemberRole.OWNER) {
      throw new AccessDeniedException("Cannot change owner role");
    }

    // If promoting to admin, only owner can do it
    if (request.role() == GroupMember.MemberRole.ADMIN && !group.hasOwner(currentUserId)) {
      throw new AccessDeniedException("Only group owner can promote members to admin");
    }

    member.setRole(request.role());
    GroupMember savedMember = groupMemberRepository.save(member);

    log.info("Updated role of member {} in group {} to {} by {}",
        memberId, groupId, request.role(), currentUserId);

    return GroupMemberDto.from(savedMember);
  }

  /**
   * Remove member from group (admin only, or member can remove themselves)
   */
  public void removeMember(UUID groupId, UUID memberId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    GroupMember member = groupMemberRepository.findById(memberId)
        .orElseThrow(() -> new MemberNotFoundException("Member not found: " + memberId));

    // Check permissions: admin can remove anyone (except owner), member can remove
    // themselves
    boolean isAdmin = group.hasAdmin(currentUserId);
    boolean isSelfRemoval = member.getUserId().equals(currentUserId);

    if (!isAdmin && !isSelfRemoval) {
      throw new AccessDeniedException("You don't have permission to remove this member");
    }

    // Owner cannot be removed
    if (member.getRole() == GroupMember.MemberRole.OWNER) {
      throw new AccessDeniedException("Cannot remove group owner");
    }

    if (isSelfRemoval) {
      member.leave();
    } else {
      member.remove();
    }

    groupMemberRepository.save(member);

    log.info("Removed member {} from group {} by {}", memberId, groupId, currentUserId);
  }

  /**
   * Leave group (member removes themselves)
   */
  public void leaveGroup(UUID groupId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
        .orElseThrow(() -> new MemberNotFoundException("You are not a member of this group"));

    if (member.getRole() == GroupMember.MemberRole.OWNER) {
      throw new AccessDeniedException("Owner cannot leave the group. Transfer ownership first or delete the group.");
    }

    member.leave();
    groupMemberRepository.save(member);

    log.info("User {} left group {}", currentUserId, groupId);
  }

  /**
   * Get user's memberships
   */
  @Transactional(readOnly = true)
  public List<GroupMemberDto> getUserMemberships(Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    return groupMemberRepository.findActiveByUserId(currentUserId)
        .stream()
        .map(GroupMemberDto::from)
        .toList();
  }

  /**
   * Transfer ownership (owner only)
   */
  public void transferOwnership(UUID groupId, UUID newOwnerId, Authentication authentication) {
    UUID currentUserId = getCurrentUserId(authentication);

    Group group = groupRepository.findActiveById(groupId)
        .orElseThrow(() -> new GroupNotFoundException("Group not found: " + groupId));

    // Check if current user is owner
    if (!group.hasOwner(currentUserId)) {
      throw new AccessDeniedException("Only group owner can transfer ownership");
    }

    // Get current owner and new owner members
    GroupMember currentOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
        .orElseThrow(() -> new MemberNotFoundException("Current owner membership not found"));

    GroupMember newOwner = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
        .orElseThrow(() -> new MemberNotFoundException("New owner is not a member of this group"));

    // Transfer ownership
    currentOwner.setRole(GroupMember.MemberRole.ADMIN);
    newOwner.setRole(GroupMember.MemberRole.OWNER);

    groupMemberRepository.save(currentOwner);
    groupMemberRepository.save(newOwner);

    log.info("Transferred ownership of group {} from {} to {}", groupId, currentUserId, newOwnerId);
  }

  // Helper methods
  private UUID getCurrentUserId(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    return UUID.fromString(jwt.getSubject());
  }

  private boolean hasViewAccess(Group group, UUID userId) {
    return group.hasMember(userId) ||
        (group.getSettings() != null && group.getSettings().isPubliclyViewable());
  }
}
