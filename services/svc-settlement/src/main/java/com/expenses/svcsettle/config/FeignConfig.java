package com.expenses.svcsettle.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign configuration (minimal) adding JWT propagation so downstream group service calls
 * receive the caller's Authorization header. Mirrors logic in expense service.
 */
@Configuration
@EnableFeignClients(basePackages = "com.expenses.svcsettle.client")
@Slf4j
public class FeignConfig {

  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Bean
  public RequestInterceptor jwtForwardingInterceptor() {
    return template -> {
      if (template.headers().containsKey(AUTH_HEADER)) {
        return; // don't overwrite
      }
      String token = resolveJwtFromSecurityContext();
      if (token == null) {
        token = resolveJwtFromServletRequest();
      }
      if (token != null && !token.isBlank()) {
        template.header(AUTH_HEADER, BEARER_PREFIX + token);
        log.trace("Feign interceptor: Authorization propagated for {}", template.url());
      }
    };
  }

  private String resolveJwtFromSecurityContext() {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth instanceof JwtAuthenticationToken jwtAuth) {
        Jwt jwt = jwtAuth.getToken();
        return jwt != null ? jwt.getTokenValue() : null;
      }
    } catch (RuntimeException ignored) {
      // ignore
    }
    return null;
  }

  private String resolveJwtFromServletRequest() {
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      return null;
    }
    HttpServletRequest request = attrs.getRequest();
    String header = request.getHeader(AUTH_HEADER);
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      return header.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
