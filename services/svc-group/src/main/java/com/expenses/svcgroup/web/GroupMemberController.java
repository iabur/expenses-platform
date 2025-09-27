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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcgroup.dto.GroupMemberDto;
import com.expenses.svcgroup.service.GroupMemberService;
import com.expenses.svcgroup.service.GroupService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups/{groupId}/members")
@RequiredArgsConstructor
@Tag(name = "Group Members", description = "Group membership management operations for adding, removing, and managing group members")
public class GroupMemberController {

  private final GroupMemberService groupMemberService;

  @PostMapping
  @Operation(summary = "Add member to group", description = "Adds a user to the group with a specified role. Only group owners and administrators can add new members. The user must exist in the system and not already be a member of the group.", operationId = "addMember", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Member added successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupMemberDto.class), examples = @ExampleObject(name = "Added Member", value = """
          {
            "id": "member-123",
            "userId": "123e4567-e89b-12d3-a456-426614174000",
            "groupId": "550e8400-e29b-41d4-a716-446655440000",
            "role": "MEMBER",
            "joinedAt": "2024-01-15T10:30:00Z",
            "invitedBy": "456e7890-e89b-12d3-a456-426614174001"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group or user not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "User already a member of this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<GroupMemberDto> addMember(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @Valid @RequestBody GroupMemberDto.AddMemberRequest request,
      Authentication authentication) {

    GroupMemberDto member = groupMemberService.addMember(groupId, request, authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(member);
  }

  @GetMapping
  @Operation(summary = "Get group members", description = "Retrieves a list of all active members in the group, including their roles, join dates, and member information. Only group members can view this information.", operationId = "getGroupMembers", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Members retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.List.class), examples = @ExampleObject(name = "Members List", value = """
          [
            {
              "id": "member-123",
              "userId": "123e4567-e89b-12d3-a456-426614174000",
              "role": "OWNER",
              "joinedAt": "2024-01-15T10:30:00Z",
              "user": {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "displayName": "John Doe",
                "email": "john@example.com"
              }
            }
          ]
          """))),
      @ApiResponse(responseCode = "403", description = "Access denied - user is not a member of this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<List<GroupMemberDto>> getGroupMembers(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      Authentication authentication) {

    List<GroupMemberDto> members = groupMemberService.getGroupMembers(groupId, authentication);
    return ResponseEntity.ok(members);
  }

  @PutMapping("/{memberId}/role")
  @Operation(summary = "Update member role", description = "Updates the role of a group member. Only group owners can change member roles. Owners cannot demote themselves, and there must always be at least one owner in the group.", operationId = "updateMemberRole", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Member role updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupMemberDto.class), examples = @ExampleObject(name = "Updated Member", value = """
          {
            "id": "member-123",
            "userId": "123e4567-e89b-12d3-a456-426614174000",
            "role": "ADMIN",
            "updatedAt": "2024-01-20T14:45:00Z"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Access denied - only group owner can change roles", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Member not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<GroupMemberDto> updateMemberRole(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @Parameter(description = "Unique identifier of the member", required = true, example = "member-123") @PathVariable UUID memberId,
      @Valid @RequestBody GroupMemberDto.UpdateMemberRoleRequest request,
      Authentication authentication) {

    GroupMemberDto member = groupMemberService.updateMemberRole(groupId, memberId, request, authentication);
    return ResponseEntity.ok(member);
  }

  @DeleteMapping("/{memberId}")
  @Operation(summary = "Remove member from group", description = "Removes a member from the group. Members can remove themselves, and admins can remove other members. Group owners cannot be removed unless ownership is transferred first.", operationId = "removeMember", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Member removed successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Member not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> removeMember(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @Parameter(description = "Unique identifier of the member", required = true, example = "member-123") @PathVariable UUID memberId,
      Authentication authentication) {

    groupMemberService.removeMember(groupId, memberId, authentication);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/leave")
  @Operation(summary = "Leave group", description = "Allows the authenticated user to leave the group. Group owners must transfer ownership before leaving. This action cannot be undone and the user will lose access to all group expenses and settlements.", operationId = "leaveGroup", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Left group successfully"),
      @ApiResponse(responseCode = "403", description = "Cannot leave - owner must transfer ownership first", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Not a member of the group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> leaveGroup(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      Authentication authentication) {

    groupMemberService.leaveGroup(groupId, authentication);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/transfer-ownership/{newOwnerId}")
  @Operation(summary = "Transfer ownership", description = "Transfers group ownership from the current owner to another member. Only the current owner can perform this action. The new owner must already be a member of the group.", operationId = "transferOwnership", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Ownership transferred successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - only current owner can transfer ownership", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "New owner not found or not a member of the group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> transferOwnership(
      @Parameter(description = "Unique identifier of the group", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @Parameter(description = "User ID of the new owner", required = true, example = "123e4567-e89b-12d3-a456-426614174000") @PathVariable UUID newOwnerId,
      Authentication authentication) {

    groupMemberService.transferOwnership(groupId, newOwnerId, authentication);
    return ResponseEntity.noContent().build();
  }
}

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "User Memberships", description = "Operations for managing user's group memberships and retrieving membership information")
class UserMembershipController {

  private final GroupMemberService groupMemberService;

  @GetMapping("/my")
  @Operation(summary = "Get user memberships", description = "Retrieves all groups where the authenticated user is an active member, including membership details such as role, join date, and group information.", operationId = "getUserMemberships", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Memberships retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.List.class), examples = @ExampleObject(name = "User Memberships", value = """
          [
            {
              "id": "member-123",
              "groupId": "550e8400-e29b-41d4-a716-446655440000",
              "role": "OWNER",
              "joinedAt": "2024-01-15T10:30:00Z",
              "group": {
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "name": "Weekend Trip",
                "description": "Shared expenses for our weekend getaway",
                "currency": "USD"
              }
            }
          ]
          """))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<List<GroupMemberDto>> getUserMemberships(Authentication authentication) {

    List<GroupMemberDto> memberships = groupMemberService.getUserMemberships(authentication);
    return ResponseEntity.ok(memberships);
  }
}

/**
 * Internal API endpoints for service-to-service communication
 */
@RestController
@RequestMapping("/groups/{groupId}")
@RequiredArgsConstructor
@Tag(name = "Group Internal API", description = "Internal endpoints for service-to-service communication")
class GroupInternalController {

  private final GroupService groupService;

  @GetMapping("/members/check")
  @Operation(summary = "Check if user is member", description = "Internal endpoint to check if a user is a member of the group")
  public ResponseEntity<Boolean> checkMembership(
      @PathVariable UUID groupId,
      @RequestParam UUID userId) {
    
    boolean isMember = groupService.isUserMemberOfGroup(groupId, userId);
    return ResponseEntity.ok(isMember);
  }

  @GetMapping("/admin/check")
  @Operation(summary = "Check if user is admin", description = "Internal endpoint to check if a user is an admin of the group")
  public ResponseEntity<Boolean> checkAdminStatus(
      @PathVariable UUID groupId,
      @RequestParam UUID userId) {
    
    boolean isAdmin = groupService.isUserAdminOfGroup(groupId, userId);
    return ResponseEntity.ok(isAdmin);
  }

  @GetMapping("/currency")
  @Operation(summary = "Get group currency", description = "Internal endpoint to get the default currency of the group")
  public ResponseEntity<String> getGroupCurrency(@PathVariable UUID groupId) {
    
    String currency = groupService.getGroupCurrency(groupId);
    return ResponseEntity.ok(currency);
  }
}
