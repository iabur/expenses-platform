package com.expenses.svcuser.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.expenses.svcuser.entity.User;
import com.expenses.svcuser.entity.UserPreferences;
import com.expenses.svcuser.repository.UserRepository;

@Service
@Transactional
public class UserService {

  private final UserRepository userRepository;

  @Autowired
  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Get or create user from JWT token
   */
  public User getOrCreateUserFromJwt(Authentication authentication) {
    Jwt jwt = (Jwt) authentication.getPrincipal();
    String keycloakUserId = jwt.getSubject();
    String email = jwt.getClaimAsString("email");
    String firstName = jwt.getClaimAsString("given_name");
    String lastName = jwt.getClaimAsString("family_name");

    return userRepository.findByKeycloakUserId(keycloakUserId)
        .orElseGet(() -> {
          User newUser = new User(keycloakUserId, email, firstName, lastName);

          // Create default preferences
          UserPreferences preferences = new UserPreferences(newUser);
          newUser.setPreferences(preferences);

          return userRepository.save(newUser);
        });
  }

  /**
   * Get user by ID
   */
  @Transactional(readOnly = true)
  public Optional<User> findById(UUID id) {
    return userRepository.findById(id);
  }

  /**
   * Get user by email
   */
  @Transactional(readOnly = true)
  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  /**
   * Get user by Keycloak user ID
   */
  @Transactional(readOnly = true)
  public Optional<User> findByKeycloakUserId(String keycloakUserId) {
    return userRepository.findByKeycloakUserId(keycloakUserId);
  }

  /**
   * Update user profile
   */
  public User updateProfile(UUID userId, String firstName, String lastName,
      String displayName, String defaultCurrency, String locale, String timezone) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (firstName != null)
      user.setFirstName(firstName);
    if (lastName != null)
      user.setLastName(lastName);
    if (displayName != null)
      user.setDisplayName(displayName);
    if (defaultCurrency != null)
      user.setDefaultCurrency(defaultCurrency);
    if (locale != null)
      user.setLocale(locale);
    if (timezone != null)
      user.setTimezone(timezone);

    return userRepository.save(user);
  }

  /**
   * Update user preferences
   */
  public User updatePreferences(UUID userId, UserPreferences.DigestFrequency digestFrequency,
      Boolean notificationEmail, Boolean notificationPush, Boolean notificationSms) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    UserPreferences preferences = user.getPreferences();
    if (preferences == null) {
      preferences = new UserPreferences(user);
      user.setPreferences(preferences);
    }

    if (digestFrequency != null)
      preferences.setDigestFrequency(digestFrequency);
    if (notificationEmail != null)
      preferences.setNotificationEmail(notificationEmail);
    if (notificationPush != null)
      preferences.setNotificationPush(notificationPush);
    if (notificationSms != null)
      preferences.setNotificationSms(notificationSms);

    return userRepository.save(user);
  }

  /**
   * Search users
   */
  @Transactional(readOnly = true)
  public Page<User> searchUsers(String search, Pageable pageable) {
    if (search == null || search.trim().isEmpty()) {
      return userRepository.findAllActiveUsers(pageable);
    }
    return userRepository.searchActiveUsers(search.trim(), pageable);
  }

  /**
   * Get users by IDs
   */
  @Transactional(readOnly = true)
  public List<User> findUsersByIds(List<UUID> userIds) {
    return userRepository.findAllById(userIds);
  }

  /**
   * Deactivate user (soft delete)
   */
  public void deactivateUser(UUID userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    user.setIsActive(false);
    userRepository.save(user);
  }

  /**
   * Check if user exists by email
   */
  @Transactional(readOnly = true)
  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  /**
   * Get current user from authentication context
   */
  @Transactional(readOnly = true)
  public Optional<User> getCurrentUser(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) {
      return Optional.empty();
    }

    Jwt jwt = (Jwt) authentication.getPrincipal();
    String keycloakUserId = jwt.getSubject();

    return userRepository.findByKeycloakUserId(keycloakUserId);
  }
}
