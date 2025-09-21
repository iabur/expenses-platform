package com.expenses.svcuser.web.dto;

import java.util.UUID;

import com.expenses.svcuser.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserSummaryResponse", description = "Compact user representation for cross-service lookups.")
public record UserSummaryResponse(
    @Schema(description = "Unique identifier of the user.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Email address associated with the user.", example = "jane.doe@example.com")
    String email,

    @Schema(description = "Display name or full name of the user.", example = "Jane Doe")
    String displayName,

    @Schema(description = "Preferred ISO-4217 currency code.", example = "USD")
    String defaultCurrency,

    @Schema(description = "Avatar URL if available.", example = "https://cdn.example.com/avatars/jane.png", nullable = true)
    String avatarUrl) {

  public static UserSummaryResponse from(User user) {
    return new UserSummaryResponse(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getDefaultCurrency(),
        user.getAvatarUrl());
  }
}
