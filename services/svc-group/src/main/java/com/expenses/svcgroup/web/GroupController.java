package com.expenses.svcgroup.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.expenses.svcgroup.dto.GroupDto;
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
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management operations for creating, updating, and managing expense sharing groups")
public class GroupController {

  private final GroupService groupService;

  @PostMapping
  @Operation(summary = "Create a new group", description = "Creates a new expense sharing group with the authenticated user as the owner. The group can be configured with specific settings like default currency, description, and privacy level.", operationId = "createGroup", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Group created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDto.class), examples = @ExampleObject(name = "Success Response", value = """
          {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Weekend Trip",
            "description": "Shared expenses for our weekend getaway",
            "currency": "USD",
            "ownerId": "123e4567-e89b-12d3-a456-426614174000",
            "createdAt": "2024-01-15T10:30:00Z",
            "memberCount": 1,
            "isActive": true
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<GroupDto> createGroup(
      @Valid @RequestBody GroupDto.CreateGroupRequest request,
      Authentication authentication) {

    GroupDto group = groupService.createGroup(request, authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(group);
  }

  @GetMapping("/{groupId}")
  @Operation(summary = "Get group by ID", description = "Retrieves detailed information about a specific group including its settings, member list, and current statistics. Only group members can access this information.", operationId = "getGroup", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Group retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDto.class), examples = @ExampleObject(name = "Group Details", value = """
          {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Weekend Trip",
            "description": "Shared expenses for our weekend getaway",
            "currency": "USD",
            "ownerId": "123e4567-e89b-12d3-a456-426614174000",
            "createdAt": "2024-01-15T10:30:00Z",
            "updatedAt": "2024-01-20T14:45:00Z",
            "memberCount": 4,
            "isActive": true,
            "members": [
              {
                "userId": "123e4567-e89b-12d3-a456-426614174000",
                "role": "OWNER",
                "joinedAt": "2024-01-15T10:30:00Z"
              }
            ]
          }
          """))),
      @ApiResponse(responseCode = "403", description = "Access denied - user is not a member of this group", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<GroupDto> getGroup(
      @Parameter(description = "Unique identifier of the group to retrieve", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      Authentication authentication) {

    GroupDto group = groupService.getGroup(groupId, authentication);
    return ResponseEntity.ok(group);
  }

  @PutMapping("/{groupId}")
  @Operation(summary = "Update group", description = "Updates group information such as name, description, and currency settings. Only group owners and administrators can perform this operation.", operationId = "updateGroup", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Group updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GroupDto.class), examples = @ExampleObject(name = "Updated Group", value = """
          {
            "id": "550e8400-e29b-41d4-a716-446655440000",
            "name": "Updated Weekend Trip",
            "description": "Updated description for our weekend getaway",
            "currency": "EUR",
            "ownerId": "123e4567-e89b-12d3-a456-426614174000",
            "updatedAt": "2024-01-20T14:45:00Z"
          }
          """))),
      @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<GroupDto> updateGroup(
      @Parameter(description = "Unique identifier of the group to update", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      @Valid @RequestBody GroupDto.UpdateGroupRequest request,
      Authentication authentication) {

    GroupDto group = groupService.updateGroup(groupId, request, authentication);
    return ResponseEntity.ok(group);
  }

  @DeleteMapping("/{groupId}")
  @Operation(summary = "Delete group", description = "Soft deletes a group, marking it as inactive. This operation can only be performed by the group owner. Deleted groups cannot be restored, and all associated expenses and settlements will be archived.", operationId = "deleteGroup", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Group deleted successfully"),
      @ApiResponse(responseCode = "403", description = "Access denied - only group owner can delete", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Void> deleteGroup(
      @Parameter(description = "Unique identifier of the group to delete", required = true, example = "550e8400-e29b-41d4-a716-446655440000") @PathVariable UUID groupId,
      Authentication authentication) {

    groupService.deleteGroup(groupId, authentication);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/my")
  @Operation(summary = "Get user's groups", description = "Retrieves a paginated list of all groups where the authenticated user is an active member. Results are ordered by most recent activity.", operationId = "getUserGroups", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User groups retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<GroupDto>> getUserGroups(
      Authentication authentication,
      Pageable pageable) {

    Page<GroupDto> groups = groupService.getUserGroups(authentication, pageable);
    return ResponseEntity.ok(groups);
  }

  @GetMapping("/search")
  @Operation(summary = "Search groups", description = "Searches for groups by name or description using a text query. Results are filtered to only include groups where the user is a member or public groups. Supports pagination for large result sets.", operationId = "searchGroups", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Search results retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.data.domain.Page.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<Page<GroupDto>> searchGroups(
      @Parameter(description = "Search query to match against group name or description", example = "weekend trip") @RequestParam(required = false) String q,
      Authentication authentication,
      Pageable pageable) {

    Page<GroupDto> groups = groupService.searchGroups(q, authentication, pageable);
    return ResponseEntity.ok(groups);
  }

  @GetMapping("/admin")
  @Operation(summary = "Get admin groups", description = "Retrieves all groups where the authenticated user has administrative privileges (owner or admin role). Useful for displaying groups where the user can manage settings and members.", operationId = "getAdminGroups", security = {
      @SecurityRequirement(name = "bearerAuth") })
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Admin groups retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = java.util.List.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class))),
      @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
  })
  public ResponseEntity<java.util.List<GroupDto>> getAdminGroups(Authentication authentication) {

    java.util.List<GroupDto> groups = groupService.getGroupsWhereUserIsAdmin(authentication);
    return ResponseEntity.ok(groups);
  }
}
