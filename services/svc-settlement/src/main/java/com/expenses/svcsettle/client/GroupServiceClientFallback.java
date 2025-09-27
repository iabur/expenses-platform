package com.expenses.svcsettle.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Fallback implementation for GroupServiceClient when the service is unavailable
 */
@Component
@Slf4j
public class GroupServiceClientFallback implements GroupServiceClient {

  @Override
  public boolean isUserMemberOfGroup(UUID groupId, UUID userId) {
    log.warn("Group Service unavailable - failing open for membership check: user {} in group {}", 
        userId, groupId);
    // Fail open - allow access if group service is unavailable
    return true;
  }

  @Override
  public boolean isUserAdminOfGroup(UUID groupId, UUID userId) {
    log.warn("Group Service unavailable - failing closed for admin check: user {} in group {}", 
        userId, groupId);
    // Fail closed - deny admin access if group service is unavailable
    return false;
  }

  @Override
  public String getGroupCurrency(UUID groupId) {
    log.warn("Group Service unavailable - using default currency USD for group {}", groupId);
    // Return default currency if group service is unavailable
    return "USD";
  }
}
