package com.expenses.gateway.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

  @Bean
  public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeExchange(exchanges -> exchanges
            // Public endpoints
            .pathMatchers(HttpMethod.GET, "/actuator/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/health/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/internal/api-docs/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
            .pathMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/webjars/**").permitAll()
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers("/fallback/**").permitAll()

            // API endpoints require authentication
            .pathMatchers("/api/**").authenticated()
            .pathMatchers("/internal/**").authenticated()

            // Everything else requires authentication
            .anyExchange().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> {
            }))
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allow specific origins (configure based on environment)
    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "https://*.expenses-platform.com",
        "https://expenses-platform.com"));

    // Allow specific methods
    configuration.setAllowedMethods(Arrays.asList(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.OPTIONS.name()));

    // Allow all headers
    configuration.setAllowedHeaders(List.of("*"));

    // Allow credentials (cookies, authorization headers)
    configuration.setAllowCredentials(true);

    // Cache preflight response for 1 hour
    configuration.setMaxAge(Duration.ofHours(1));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
  }
}
