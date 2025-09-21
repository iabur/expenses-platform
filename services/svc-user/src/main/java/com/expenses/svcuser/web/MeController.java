package com.expenses.svcuser.web;

import java.util.Optional;
import java.util.UUID;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expenses.svcuser.entity.User;
import com.expenses.svcuser.entity.UserPreferences;
import com.expenses.svcuser.exception.GlobalExceptionHandler.ErrorResponse;
import com.expenses.svcuser.exception.UserNotFoundException;
import com.expenses.svcuser.service.UserService;
import com.expenses.svcuser.web.dto.UserPreferencesResponse;
import com.expenses.svcuser.web.dto.UserPreferencesUpdateRequest;
import com.expenses.svcuser.web.dto.UserProfileResponse;
import com.expenses.svcuser.web.dto.UserProfileUpdateRequest;
import com.expenses.svcuser.web.dto.UserSearchResponse;
import com.expenses.svcuser.web.dto.UserSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/user")
@Tag(name = "User Management", description = "Operations for managing user identities, profile data, preferences, and user discovery.")
public class MeController {

  private final UserService userService;

  @Autowired
  public MeController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping("/me")
  @Operation(
      summary = "Get current user profile",
      description = "Returns the enriched profile for the authenticated principal. The profile is provisioned lazily from JWT claims when the user signs in for the first time.",
      operationId = "getCurrentUserProfile",
      security = {@SecurityRequirement(name = "bearerAuth")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Unexpected error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
    User user = userService.getOrCreateUserFromJwt(authentication);
    return ResponseEntity.ok(UserProfileResponse.from(user));
  }

  @PutMapping("/me")
  @Operation(
      summary = "Update current user profile",
      description = "Updates mutable profile attributes such as display name, locale, timezone, and default currency. Only provided fields are updated.",
      operationId = "updateCurrentUserProfile",
      security = {@SecurityRequirement(name = "bearerAuth")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Profile updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserProfileResponse.class))),
      @ApiResponse(responseCode = "400", description = "Validation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "User was not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Unexpected error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserProfileResponse> updateProfile(
      @Valid @RequestBody UserProfileUpdateRequest request,
      Authentication authentication) {

    User currentUser = userService.getOrCreateUserFromJwt(authentication);

    User updatedUser = userService.updateProfile(
        currentUser.getId(),
        request.firstName(),
        request.lastName(),
        request.displayName(),
        request.defaultCurrency(),
        request.locale(),
        request.timezone());

    return ResponseEntity.ok(UserProfileResponse.from(updatedUser));
  }

  @GetMapping("/me/preferences")
  @Operation(
      summary = "Get current user preferences",
      description = "Returns notification preferences for the authenticated user, including digest cadence and quiet hours.",
      operationId = "getCurrentUserPreferences",
      security = {@SecurityRequirement(name = "bearerAuth")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPreferencesResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Unexpected error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserPreferencesResponse> getPreferences(Authentication authentication) {
    User user = userService.getOrCreateUserFromJwt(authentication);
    UserPreferences preferences = Optional.ofNullable(user.getPreferences()).orElseGet(() -> new UserPreferences(user));
    return ResponseEntity.ok(UserPreferencesResponse.from(preferences));
  }

  @PutMapping("/me/preferences")
  @Operation(
      summary = "Update current user preferences",
      description = "Updates notification toggles and digest cadence for the authenticated user.",
      operationId = "updateCurrentUserPreferences",
      security = {@SecurityRequirement(name = "bearerAuth")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Preferences updated successfully",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserPreferencesResponse.class))),
      @ApiResponse(responseCode = "400", description = "Validation failed",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "User was not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Unexpected error",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserPreferencesResponse> updatePreferences(
      @Valid @RequestBody UserPreferencesUpdateRequest request,
      Authentication authentication) {

    User currentUser = userService.getOrCreateUserFromJwt(authentication);

    User updatedUser = userService.updatePreferences(
        currentUser.getId(),
        request.digestFrequency(),
        request.notificationEmail(),
        request.notificationPush(),
        request.notificationSms());

    UserPreferences preferences = Optional.ofNullable(updatedUser.getPreferences()).orElseGet(() -> new UserPreferences(updatedUser));
    return ResponseEntity.ok(UserPreferencesResponse.from(preferences));
  }

  @GetMapping("/search")
  @Operation(
      summary = "Search users",
      description = "Performs a paginated search across active users by name or email. Use this endpoint when inviting collaborators or sharing expenses.",
      operationId = "searchUsers",
      security = {@SecurityRequirement(name = "bearerAuth")}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Search results returned",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserSearchResponse.class))),
      @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserSearchResponse> searchUsers(
      @Parameter(description = "Query used to match against display name and email. When omitted, all active users are returned.", example = "alex")
      @RequestParam(required = false) String q,
      @ParameterObject
      @PageableDefault(size = 10, sort = "displayName") Pageable pageable) {

    Page<User> users = userService.searchUsers(q, pageable);
    return ResponseEntity.ok(UserSearchResponse.from(users));
  }

  @GetMapping("/{userId}")
  @Operation(
      summary = "Get user by ID",
      description = "Retrieves a compact user representation by identifier. Intended for service-to-service lookups.",
      operationId = "getUserById",
      security = {}
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "User found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserSummaryResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid user identifier",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
      @ApiResponse(responseCode = "404", description = "User was not found",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
  })
  public ResponseEntity<UserSummaryResponse> getUserById(
      @Parameter(description = "Unique identifier of the user to retrieve.", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
      @PathVariable UUID userId) {
    User user = userService.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

    return ResponseEntity.ok(UserSummaryResponse.from(user));
  }
}
