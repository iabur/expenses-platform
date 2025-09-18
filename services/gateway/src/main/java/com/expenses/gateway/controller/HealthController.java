package com.expenses.gateway.controller;

import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class HealthController {

  @GetMapping
  public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> health = Map.of(
        "status", "UP",
        "service", "expenses-platform-gateway",
        "timestamp", ZonedDateTime.now(),
        "version", "1.0.0",
        "components", Map.of(
            "gateway", "UP",
            "circuitBreaker", "UP",
            "rateLimiter", "UP",
            "loadBalancer", "UP"));

    return ResponseEntity.ok(health);
  }

  @GetMapping("/ready")
  public ResponseEntity<Map<String, Object>> readiness() {
    Map<String, Object> ready = Map.of(
        "status", "READY",
        "message", "Gateway is ready to accept requests",
        "timestamp", ZonedDateTime.now());

    return ResponseEntity.ok(ready);
  }

  @GetMapping("/live")
  public ResponseEntity<Map<String, Object>> liveness() {
    Map<String, Object> live = Map.of(
        "status", "ALIVE",
        "message", "Gateway is running",
        "timestamp", ZonedDateTime.now());

    return ResponseEntity.ok(live);
  }
}
