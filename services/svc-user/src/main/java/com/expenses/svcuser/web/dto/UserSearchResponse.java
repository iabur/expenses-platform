package com.expenses.svcuser.web.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.expenses.svcuser.entity.User;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "UserSearchResponse", description = "Paginated collection of user search results.")
public record UserSearchResponse(
    @ArraySchema(schema = @Schema(implementation = UserSearchResult.class), arraySchema = @Schema(description = "Users matching the search criteria."))
    List<UserSearchResult> users,

    @Schema(description = "Total number of users matching the filter across all pages.", example = "42")
    long totalElements,

    @Schema(description = "Total number of available pages.", example = "5")
    int totalPages,

    @Schema(description = "Current page index (zero-based).", example = "0")
    int currentPage,

    @Schema(description = "Number of records requested per page.", example = "10")
    int size) {

  public static UserSearchResponse from(Page<User> page) {
    List<UserSearchResult> results = page.getContent().stream()
        .map(UserSearchResult::from)
        .toList();

    return new UserSearchResponse(
        results,
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize());
  }
}
