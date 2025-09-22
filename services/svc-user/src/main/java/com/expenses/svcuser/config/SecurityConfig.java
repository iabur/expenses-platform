package com.expenses.svcuser.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for User Service.
 * 
 * Configures:
 * - JWT-based authentication using OAuth2 Resource Server
 * - CORS support for cross-origin requests
 * - Public endpoints for health checks and API documentation
 * - Stateless session management
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  /**
   * Configures the security filter chain for the application.
   * 
   * @param http HttpSecurity configuration
   * @return SecurityFilterChain
   * @throws Exception if configuration fails
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        // Enable CORS with default configuration (uses CorsConfig bean)
        .cors(withDefaults())
        // Disable CSRF for stateless API
        .csrf(AbstractHttpConfigurer::disable)
        // Configure stateless session management
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Configure authorization rules
        .authorizeHttpRequests(auth -> auth
            // Public endpoints
            .requestMatchers(
                "/actuator/**", // Health checks and metrics
                "/v3/api-docs/**", // OpenAPI documentation
                "/swagger-ui.html", // Swagger UI
                "/swagger-ui/**" // Swagger UI resources
            ).permitAll()
            // All other requests require authentication
            .anyRequest().authenticated())
        // Configure OAuth2 Resource Server for JWT validation
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))
        .build();
  }
}
