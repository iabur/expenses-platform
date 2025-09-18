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
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management operations")
public class GroupController {

  private final GroupService groupService;

  @PostMapping
  @Operation(summary = "Create a new group", description = "Creates a new group with the current user as owner")
  @ApiResponse(responseCode = "201", description = "Group created successfully")
  public ResponseEntity<GroupDto> createGroup(
      @Valid @RequestBody GroupDto.CreateGroupRequest request,
      Authentication authentication) {

    GroupDto group = groupService.createGroup(request, authentication);
    return ResponseEntity.status(HttpStatus.CREATED).body(group);
  }

  @GetMapping("/{groupId}")
  @Operation(summary = "Get group by ID", description = "Retrieves group details including members and settings")
  @ApiResponse(responseCode = "200", description = "Group retrieved successfully")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  public ResponseEntity<GroupDto> getGroup(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    GroupDto group = groupService.getGroup(groupId, authentication);
    return ResponseEntity.ok(group);
  }

  @PutMapping("/{groupId}")
  @Operation(summary = "Update group", description = "Updates group information (admin only)")
  @ApiResponse(responseCode = "200", description = "Group updated successfully")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  public ResponseEntity<GroupDto> updateGroup(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      @Valid @RequestBody GroupDto.UpdateGroupRequest request,
      Authentication authentication) {

    GroupDto group = groupService.updateGroup(groupId, request, authentication);
    return ResponseEntity.ok(group);
  }

  @DeleteMapping("/{groupId}")
  @Operation(summary = "Delete group", description = "Soft deletes a group (owner only)")
  @ApiResponse(responseCode = "204", description = "Group deleted successfully")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @ApiResponse(responseCode = "403", description = "Access denied")
  public ResponseEntity<Void> deleteGroup(
      @Parameter(description = "Group ID") @PathVariable UUID groupId,
      Authentication authentication) {

    groupService.deleteGroup(groupId, authentication);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/my")
  @Operation(summary = "Get user's groups", description = "Retrieves all groups where the current user is a member")
  @ApiResponse(responseCode = "200", description = "User groups retrieved successfully")
  public ResponseEntity<Page<GroupDto>> getUserGroups(
      Authentication authentication,
      Pageable pageable) {

    Page<GroupDto> groups = groupService.getUserGroups(authentication, pageable);
    return ResponseEntity.ok(groups);
  }

  @GetMapping("/search")
  @Operation(summary = "Search groups", description = "Searches for groups by name or description")
  @ApiResponse(responseCode = "200", description = "Search results retrieved successfully")
  public ResponseEntity<Page<GroupDto>> searchGroups(
      @Parameter(description = "Search query") @RequestParam(required = false) String q,
      Authentication authentication,
      Pageable pageable) {

    Page<GroupDto> groups = groupService.searchGroups(q, authentication, pageable);
    return ResponseEntity.ok(groups);
  }

  @GetMapping("/admin")
  @Operation(summary = "Get admin groups", description = "Retrieves groups where the current user is an admin")
  @ApiResponse(responseCode = "200", description = "Admin groups retrieved successfully")
  public ResponseEntity<java.util.List<GroupDto>> getAdminGroups(Authentication authentication) {

    java.util.List<GroupDto> groups = groupService.getGroupsWhereUserIsAdmin(authentication);
    return ResponseEntity.ok(groups);
  }
}
