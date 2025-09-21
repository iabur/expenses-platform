package com.expenses.svcuser.web.dto;

import java.time.LocalTime;
import java.time.ZonedDateTime;

import com.expenses.svcuser.entity.UserPreferences;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserPreferencesResponse", description = "Notification and digest preferences configured for the authenticated user.")
public record UserPreferencesResponse(
    @Schema(description = "Whether transactional and reminder emails are enabled.", example = "true")
    Boolean notificationEmail,

    @Schema(description = "Whether push notifications are enabled.", example = "false")
    Boolean notificationPush,

    @Schema(description = "Whether SMS notifications are enabled.", example = "false")
    Boolean notificationSms,

    @Schema(description = "Frequency for aggregate digest notifications.", example = "WEEKLY")
    UserPreferences.DigestFrequency digestFrequency,

    @Schema(description = "Local time when quiet hours begin (24h format).", example = "22:00", nullable = true)
    LocalTime quietHoursStart,

    @Schema(description = "Local time when quiet hours end (24h format).", example = "07:30", nullable = true)
    LocalTime quietHoursEnd,

    @Schema(description = "Timestamp when the preferences were last updated.", example = "2024-02-01T12:45:00Z", nullable = true)
    ZonedDateTime updatedAt) {

  public static UserPreferencesResponse from(UserPreferences preferences) {
    return new UserPreferencesResponse(
        preferences.getNotificationEmail(),
        preferences.getNotificationPush(),
        preferences.getNotificationSms(),
        preferences.getDigestFrequency(),
        preferences.getQuietHoursStart(),
        preferences.getQuietHoursEnd(),
        preferences.getUpdatedAt());
  }
}
