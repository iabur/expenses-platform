package com.expenses.svcuser.web;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import com.expenses.svcuser.service.UserService;

@RestController
@RequestMapping("/user")
public class MeController {

  private final UserService userService;

  @Autowired
  public MeController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Get current user profile
   */
  @GetMapping("/me")
  public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
    User user = userService.getOrCreateUserFromJwt(authentication);

    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "firstName", user.getFirstName(),
        "lastName", user.getLastName(),
        "displayName", user.getDisplayName(),
        "defaultCurrency", user.getDefaultCurrency(),
        "locale", user.getLocale(),
        "timezone", user.getTimezone(),
        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
        "createdAt", user.getCreatedAt()));
  }

  /**
   * Update current user profile
   */
  @PutMapping("/me")
  public ResponseEntity<Map<String, Object>> updateProfile(
      @RequestBody Map<String, Object> request,
      Authentication authentication) {

    User currentUser = userService.getOrCreateUserFromJwt(authentication);

    User updatedUser = userService.updateProfile(
        currentUser.getId(),
        (String) request.get("firstName"),
        (String) request.get("lastName"),
        (String) request.get("displayName"),
        (String) request.get("defaultCurrency"),
        (String) request.get("locale"),
        (String) request.get("timezone"));

    return ResponseEntity.ok(Map.of(
        "id", updatedUser.getId(),
        "email", updatedUser.getEmail(),
        "firstName", updatedUser.getFirstName(),
        "lastName", updatedUser.getLastName(),
        "displayName", updatedUser.getDisplayName(),
        "defaultCurrency", updatedUser.getDefaultCurrency(),
        "locale", updatedUser.getLocale(),
        "timezone", updatedUser.getTimezone(),
        "updatedAt", updatedUser.getUpdatedAt()));
  }

  /**
   * Get current user preferences
   */
  @GetMapping("/me/preferences")
  public ResponseEntity<Map<String, Object>> getPreferences(Authentication authentication) {
    User user = userService.getOrCreateUserFromJwt(authentication);
    UserPreferences prefs = user.getPreferences();

    if (prefs == null) {
      prefs = new UserPreferences(user);
    }

    return ResponseEntity.ok(Map.of(
        "notificationEmail", prefs.getNotificationEmail(),
        "notificationPush", prefs.getNotificationPush(),
        "notificationSms", prefs.getNotificationSms(),
        "digestFrequency", prefs.getDigestFrequency().name(),
        "quietHoursStart", prefs.getQuietHoursStart() != null ? prefs.getQuietHoursStart().toString() : null,
        "quietHoursEnd", prefs.getQuietHoursEnd() != null ? prefs.getQuietHoursEnd().toString() : null));
  }

  /**
   * Update current user preferences
   */
  @PutMapping("/me/preferences")
  public ResponseEntity<Map<String, Object>> updatePreferences(
      @RequestBody Map<String, Object> request,
      Authentication authentication) {

    User currentUser = userService.getOrCreateUserFromJwt(authentication);

    UserPreferences.DigestFrequency digestFrequency = null;
    if (request.get("digestFrequency") != null) {
      digestFrequency = UserPreferences.DigestFrequency.valueOf((String) request.get("digestFrequency"));
    }

    User updatedUser = userService.updatePreferences(
        currentUser.getId(),
        digestFrequency,
        (Boolean) request.get("notificationEmail"),
        (Boolean) request.get("notificationPush"),
        (Boolean) request.get("notificationSms"));

    UserPreferences prefs = updatedUser.getPreferences();
    return ResponseEntity.ok(Map.of(
        "notificationEmail", prefs.getNotificationEmail(),
        "notificationPush", prefs.getNotificationPush(),
        "notificationSms", prefs.getNotificationSms(),
        "digestFrequency", prefs.getDigestFrequency().name(),
        "updatedAt", prefs.getUpdatedAt()));
  }

  /**
   * Search users (for adding to groups/expenses)
   */
  @GetMapping("/search")
  public ResponseEntity<Map<String, Object>> searchUsers(
      @RequestParam(required = false) String q,
      Pageable pageable) {

    Page<User> users = userService.searchUsers(q, pageable);

    return ResponseEntity.ok(Map.of(
        "users", users.getContent().stream().map(user -> Map.of(
            "id", user.getId(),
            "email", user.getEmail(),
            "displayName", user.getDisplayName(),
            "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "")).toList(),
        "totalElements", users.getTotalElements(),
        "totalPages", users.getTotalPages(),
        "currentPage", users.getNumber(),
        "size", users.getSize()));
  }

  /**
   * Get user by ID (for other services to call)
   */
  @GetMapping("/{userId}")
  public ResponseEntity<Map<String, Object>> getUserById(@PathVariable UUID userId) {
    Optional<User> userOpt = userService.findById(userId);

    if (userOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    User user = userOpt.get();
    return ResponseEntity.ok(Map.of(
        "id", user.getId(),
        "email", user.getEmail(),
        "displayName", user.getDisplayName(),
        "defaultCurrency", user.getDefaultCurrency(),
        "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""));
  }
}
