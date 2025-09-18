package com.expenses.svcuser.entity;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "keycloak_user_id", unique = true, nullable = false)
  @NotBlank(message = "Keycloak user ID is required")
  private String keycloakUserId;

  @Email(message = "Email should be valid")
  @NotBlank(message = "Email is required")
  @Column(unique = true, nullable = false)
  private String email;

  @NotBlank(message = "First name is required")
  @Size(max = 100, message = "First name must be less than 100 characters")
  @Column(name = "first_name", nullable = false)
  private String firstName;

  @NotBlank(message = "Last name is required")
  @Size(max = 100, message = "Last name must be less than 100 characters")
  @Column(name = "last_name", nullable = false)
  private String lastName;

  @Size(max = 200, message = "Display name must be less than 200 characters")
  @Column(name = "display_name")
  private String displayName;

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
  @Column(name = "default_currency", length = 3, nullable = false)
  private String defaultCurrency = "USD";

  @Size(max = 10, message = "Locale must be less than 10 characters")
  @Column(length = 10)
  private String locale = "en_US";

  @Size(max = 50, message = "Timezone must be less than 50 characters")
  @Column(length = 50)
  private String timezone = "UTC";

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private UserPreferences preferences;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private Set<UserContact> contacts = new HashSet<>();

  // Constructors
  public User() {
  }

  public User(String keycloakUserId, String email, String firstName, String lastName) {
    this.keycloakUserId = keycloakUserId;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.displayName = firstName + " " + lastName;
  }

  // Getters and Setters
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getKeycloakUserId() {
    return keycloakUserId;
  }

  public void setKeycloakUserId(String keycloakUserId) {
    this.keycloakUserId = keycloakUserId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl = avatarUrl;
  }

  public String getDefaultCurrency() {
    return defaultCurrency;
  }

  public void setDefaultCurrency(String defaultCurrency) {
    this.defaultCurrency = defaultCurrency;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public ZonedDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(ZonedDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public ZonedDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(ZonedDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
  }

  public UserPreferences getPreferences() {
    return preferences;
  }

  public void setPreferences(UserPreferences preferences) {
    this.preferences = preferences;
  }

  public Set<UserContact> getContacts() {
    return contacts;
  }

  public void setContacts(Set<UserContact> contacts) {
    this.contacts = contacts;
  }

  // Helper methods
  public String getFullName() {
    return firstName + " " + lastName;
  }

  public void updateDisplayName() {
    if (displayName == null || displayName.trim().isEmpty()) {
      this.displayName = getFullName();
    }
  }

  @PrePersist
  @PreUpdate
  private void updateDisplayNameIfEmpty() {
    updateDisplayName();
  }
}
