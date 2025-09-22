package com.expenses.svcuser.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (Cross-Origin Resource Sharing) configuration for User Service.
 * 
 * Enables cross-origin requests from:
 * - Local development environments (localhost with any port)
 * - Gateway service for API aggregation
 * - Swagger UI hosted on different ports
 * 
 * This configuration works in conjunction with Spring Security's CORS
 * configuration to handle both preflight and actual requests.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  /**
   * Configures CORS mappings for all endpoints.
   * 
   * @param registry CORS registry to configure
   */
  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
        // Apply CORS to all endpoints
        .addMapping("/**")
        // Allow requests from localhost with any port (development)
        .allowedOriginPatterns(
            "http://localhost:*", // Local development
            "http://127.0.0.1:*", // Alternative localhost
            "https://localhost:*" // HTTPS local development
        )
        // Allow all standard HTTP methods
        .allowedMethods(
            "GET", // Retrieve resources
            "POST", // Create resources
            "PUT", // Update resources
            "DELETE", // Delete resources
            "PATCH", // Partial updates
            "OPTIONS" // Preflight requests
        )
        // Allow all headers (needed for Authorization, Content-Type, etc.)
        .allowedHeaders("*")
        // Allow credentials (cookies, authorization headers)
        .allowCredentials(true)
        // Cache preflight response for 1 hour
        .maxAge(3600);
  }
}
