package com.expenses.svcuser.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "UserProfileUpdateRequest", description = "Fields that can be patched on the authenticated user's profile. Omitted fields remain unchanged.")
public record UserProfileUpdateRequest(
    @Size(max = 100, message = "First name must be less than 100 characters")
    @Schema(description = "Updated given name.", example = "Jane")
    String firstName,

    @Size(max = 100, message = "Last name must be less than 100 characters")
    @Schema(description = "Updated family name.", example = "Doe")
    String lastName,

    @Size(max = 200, message = "Display name must be less than 200 characters")
    @Schema(description = "Preferred display name shown in the product UI.", example = "Jane D.")
    String displayName,

    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency code must be a valid ISO 4217 code")
    @Schema(description = "Preferred ISO-4217 currency code.", example = "EUR")
    String defaultCurrency,

    @Size(max = 10, message = "Locale must be less than 10 characters")
    @Schema(description = "Locale used for number, date, and language formatting.", example = "en-GB")
    String locale,

    @Size(max = 50, message = "Timezone must be less than 50 characters")
    @Schema(description = "IANA timezone identifier for scheduled communications.", example = "Europe/London")
    String timezone) {
}
