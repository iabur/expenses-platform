package com.expenses.gateway.controller;

import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/fallback")
@Slf4j
public class FallbackController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> fallback() {
    log.warn("Circuit breaker fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "Service Temporarily Unavailable",
        "message", "The requested service is currently experiencing issues. Please try again later.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "suggestion", "Check service status or contact support if the issue persists");

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/user")
  public ResponseEntity<Map<String, Object>> userServiceFallback() {
    log.warn("User service fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "User Service Unavailable",
        "message", "User service is temporarily unavailable. Some features may be limited.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "fallbackData", Map.of(
            "canCreateExpenses", false,
            "canViewProfile", false,
            "suggestedAction", "Try refreshing the page in a few minutes"));

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/groups")
  public ResponseEntity<Map<String, Object>> groupServiceFallback() {
    log.warn("Group service fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "Group Service Unavailable",
        "message", "Group management is temporarily unavailable.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "fallbackData", Map.of(
            "canCreateGroups", false,
            "canViewGroups", false,
            "suggestedAction", "Group operations are temporarily disabled"));

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/expenses")
  public ResponseEntity<Map<String, Object>> expenseServiceFallback() {
    log.warn("Expense service fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "Expense Service Unavailable",
        "message", "Expense management is temporarily unavailable.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "fallbackData", Map.of(
            "canCreateExpenses", false,
            "canViewExpenses", false,
            "suggestedAction", "Expense operations are temporarily disabled"));

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/splits")
  public ResponseEntity<Map<String, Object>> splitEngineServiceFallback() {
    log.warn("Split engine service fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "Split Engine Unavailable",
        "message", "Split calculations are temporarily unavailable.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "fallbackData", Map.of(
            "canCalculateSplits", false,
            "canViewBalances", false,
            "suggestedAction", "Manual split calculation may be required"));

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }

  @GetMapping("/fx")
  public ResponseEntity<Map<String, Object>> fxServiceFallback() {
    log.warn("FX service fallback triggered");

    Map<String, Object> response = Map.of(
        "error", "FX Service Unavailable",
        "message", "Currency exchange rates are temporarily unavailable.",
        "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
        "timestamp", ZonedDateTime.now(),
        "fallbackData", Map.of(
            "canGetRates", false,
            "defaultCurrency", "USD",
            "suggestedAction", "Using cached exchange rates if available"));

    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
  }
}
