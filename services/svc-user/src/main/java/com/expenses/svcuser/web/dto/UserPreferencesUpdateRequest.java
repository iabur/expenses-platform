package com.expenses.svcuser.web.dto;

import com.expenses.svcuser.entity.UserPreferences;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserPreferencesUpdateRequest", description = "User-configurable notification preferences. Only supplied fields are updated.")
public record UserPreferencesUpdateRequest(
    @Schema(description = "Desired digest cadence.", example = "DAILY")
    UserPreferences.DigestFrequency digestFrequency,

    @Schema(description = "Toggle for transactional and reminder emails.", example = "true")
    Boolean notificationEmail,

    @Schema(description = "Toggle for push notifications.", example = "false")
    Boolean notificationPush,

    @Schema(description = "Toggle for SMS notifications.", example = "false")
    Boolean notificationSms) {
}
