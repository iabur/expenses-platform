package com.expenses.svcgroup.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcgroup.dto.GroupMemberDto;
import com.expenses.svcgroup.service.GroupMemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
@Tag(name = "Group Members", description = "Group membership management operations")
public class GroupMemberController {

  private final GroupMemberService groupMemberService;

  @PostMapping
  @Operation(summary = "Add member to group", description = "Adds a user to the group (admin only)")
  @ApiResponse(responseCode = "201", description = "Member added successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @ApiResponse(responseCode = "409", description = "User already a member")
  public ResponseEntity<GroupMemberDto> addMember(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Valid @RequestBody GroupMemberDto.AddMemberRequest request,
      Authentication authentication) {

    GroupMemberDto member = groupMemberService.addMember(groupId, request, authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(member);
  }

  @GetMapping
  @Operation(summary = "Get group members", description = "Retrieves all active members of the group")
  @ApiResponse(responseCode = "200", description = "Members retrieved successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  public ResponseEntity<List<GroupMemberDto>> getGroupMembers(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    List<GroupMemberDto> members = groupMemberService.getGroupMembers(groupId, authentication);
    return ResponseEntity.ok(members);
  }

  @PutMapping("/{memberId}/role")
  @Operation(summary = "Update member role", description = "Updates the role of a group member (admin only)")
  @ApiResponse(responseCode = "200", description = "Member role updated successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @ApiResponse(responseCode = "404", description = "Member not found")
  public ResponseEntity<GroupMemberDto> updateMemberRole(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Parameter(description = "Member ID") @PathVariable UUID memberId,
      @Valid @RequestBody GroupMemberDto.UpdateMemberRoleRequest request,
      Authentication authentication) {

    GroupMemberDto member = groupMemberService.updateMemberRole(groupId, memberId, request, authentication);
    return ResponseEntity.ok(member);
  }

  @DeleteMapping("/{memberId}")
  @Operation(summary = "Remove member from group", description = "Removes a member from the group (admin only or self)")
  @ApiResponse(responseCode = "204", description = "Member removed successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @ApiResponse(responseCode = "404", description = "Member not found")
  public ResponseEntity<Void> removeMember(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Parameter(description = "Member ID") @PathVariable UUID memberId,
      Authentication authentication) {

    groupMemberService.removeMember(groupId, memberId, authentication);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/leave")
  @Operation(summary = "Leave group", description = "Current user leaves the group")
  @ApiResponse(responseCode = "204", description = "Left group successfully")
  @ApiResponse(responseCode = "403", description = "Cannot leave (owner cannot leave)")
  @ApiResponse(responseCode = "404", description = "Not a member of the group")
  public ResponseEntity<Void> leaveGroup(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    groupMemberService.leaveGroup(groupId, authentication);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/transfer-ownership/{newOwnerId}")
  @Operation(summary = "Transfer ownership", description = "Transfers group ownership to another member (owner only)")
  @ApiResponse(responseCode = "204", description = "Ownership transferred successfully")
  @ApiResponse(responseCode = "403", description = "Access denied")
  @ApiResponse(responseCode = "404", description = "New owner not found or not a member")
  public ResponseEntity<Void> transferOwnership(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Parameter(description = "New owner user ID") @PathVariable UUID newOwnerId,
      Authentication authentication) {

    groupMemberService.transferOwnership(groupId, newOwnerId, authentication);
    return ResponseEntity.noContent().build();
  }
}

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "User Memberships", description = "User's group membership operations")
class UserMembershipController {

  private final GroupMemberService groupMemberService;

  @GetMapping("/my")
  @Operation(summary = "Get user memberships", description = "Retrieves all groups where the current user is a member")
  @ApiResponse(responseCode = "200", description = "Memberships retrieved successfully")
  public ResponseEntity<List<GroupMemberDto>> getUserMemberships(Authentication authentication) {

    List<GroupMemberDto> memberships = groupMemberService.getUserMemberships(authentication);
    return ResponseEntity.ok(memberships);
  }
}
