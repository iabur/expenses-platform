package com.expenses.svcuser.web.dto;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.expenses.svcuser.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserProfileResponse", description = "Detailed view of the authenticated user's profile.")
public record UserProfileResponse(
    @Schema(description = "Unique identifier of the user.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Email address associated with the user account.", example = "jane.doe@example.com")
    String email,

    @Schema(description = "Given name sourced from identity provider claims.", example = "Jane")
    String firstName,

    @Schema(description = "Family name sourced from identity provider claims.", example = "Doe")
    String lastName,

    @Schema(description = "Display name presented across the application.", example = "Jane Doe")
    String displayName,

    @Schema(description = "Preferred ISO-4217 currency code.", example = "USD")
    String defaultCurrency,

    @Schema(description = "IETF BCP 47 locale used for formatting.", example = "en-US")
    String locale,

    @Schema(description = "IANA timezone identifier used for scheduling and notifications.", example = "America/New_York")
    String timezone,

    @Schema(description = "Publicly accessible avatar URL, if configured.", example = "https://cdn.example.com/avatars/550e8400-e29b-41d4-a716-446655440000.png", nullable = true)
    String avatarUrl,

    @Schema(description = "Timestamp when the user profile was created.", example = "2024-01-15T10:30:00Z")
    ZonedDateTime createdAt,

    @Schema(description = "Timestamp when the user profile was last updated.", example = "2024-02-01T12:45:00Z")
    ZonedDateTime updatedAt) {

  public static UserProfileResponse from(User user) {
    return new UserProfileResponse(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getDisplayName(),
        user.getDefaultCurrency(),
        user.getLocale(),
        user.getTimezone(),
        user.getAvatarUrl(),
        user.getCreatedAt(),
        user.getUpdatedAt());
  }
}
