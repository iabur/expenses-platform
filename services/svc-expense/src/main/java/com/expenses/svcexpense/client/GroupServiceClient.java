package com.expenses.svcexpense.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for communicating with Group Service
 */
@FeignClient(
    name = "group-service",
    url = "${services.group.url:http://svc-group:8082}",
    fallback = GroupServiceClientFallback.class
)
public interface GroupServiceClient {

  /**
   * Check if user is a member of the group
   */
  @GetMapping("/groups/{groupId}/members/check")
  boolean isUserMemberOfGroup(
      @PathVariable("groupId") UUID groupId,
      @RequestParam("userId") UUID userId);

  /**
   * Check if user is an admin of the group
   */
  @GetMapping("/groups/{groupId}/admin/check")
  boolean isUserAdminOfGroup(
      @PathVariable("groupId") UUID groupId,
      @RequestParam("userId") UUID userId);
}
