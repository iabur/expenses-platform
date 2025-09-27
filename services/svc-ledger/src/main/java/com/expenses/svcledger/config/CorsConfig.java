package com.expenses.svcledger.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS configuration for Ledger Service
 */
@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow requests from Gateway and other services
    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "http://gateway:*",
        "http://svc-gateway:*",
        "http://expenses-platform-gateway-1:*"));

    // Allow common HTTP methods
    configuration.setAllowedMethods(Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

    // Allow common headers
    configuration.setAllowedHeaders(Arrays.asList(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin",
        "Access-Control-Request-Method",
        "Access-Control-Request-Headers",
        "X-Request-Id",
        "X-Idempotency-Key"));

    // Allow credentials
    configuration.setAllowCredentials(true);

    // Expose common headers
    configuration.setExposedHeaders(Arrays.asList(
        "Access-Control-Allow-Origin",
        "Access-Control-Allow-Credentials",
        "X-Request-Id"));

    // Cache preflight response for 1 hour
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
