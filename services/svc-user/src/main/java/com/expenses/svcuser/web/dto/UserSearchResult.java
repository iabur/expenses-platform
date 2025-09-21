package com.expenses.svcuser.web.dto;

import java.util.UUID;

import com.expenses.svcuser.entity.User;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserSearchResult", description = "Minimal user record returned by the search endpoint.")
public record UserSearchResult(
    @Schema(description = "Unique identifier of the matched user.", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id,

    @Schema(description = "Email address used for invitations.", example = "alex@example.com")
    String email,

    @Schema(description = "Display name shown in search results.", example = "Alex Johnson")
    String displayName,

    @Schema(description = "Avatar URL if configured; empty when not set.", example = "https://cdn.example.com/avatars/alex.png", nullable = true)
    String avatarUrl) {

  public static UserSearchResult from(User user) {
    return new UserSearchResult(
        user.getId(),
        user.getEmail(),
        user.getDisplayName(),
        user.getAvatarUrl());
  }
}
