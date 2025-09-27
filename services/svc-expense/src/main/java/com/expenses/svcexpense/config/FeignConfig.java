package com.expenses.svcexpense.config;

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
 * Configuration for Feign clients
 */
@Configuration
@Slf4j
public class FeignConfig {

  private static final String AUTH_HEADER = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  @Bean
  public RequestInterceptor expenseJwtForwardingInterceptor() {
    return template -> {
      // Do not overwrite if already explicitly set somewhere else (tests, custom client)
      if (template.headers().containsKey(AUTH_HEADER)) {
        return;
      }

      String token = resolveJwtFromSecurityContext();
      if (token == null) {
        token = resolveJwtFromServletRequest();
      }

      if (token != null && !token.isBlank()) {
        template.header(AUTH_HEADER, BEARER_PREFIX + token);
        log.trace("Feign interceptor: Authorization propagated for {}", template.url());
      } else {
        // Keep at TRACE to avoid log noise; upgrade to DEBUG temporarily if needed.
        log.trace("Feign interceptor: no JWT available for {} (call may 401)", template.url());
      }
    };
  }

  private String resolveJwtFromSecurityContext() {
    Authentication auth;
    try {
      auth = SecurityContextHolder.getContext().getAuthentication();
    } catch (RuntimeException ex) { // Narrower than Exception
      return null;
    }
    if (auth instanceof JwtAuthenticationToken jwtAuth) {
      Jwt jwt = jwtAuth.getToken();
      return jwt != null ? jwt.getTokenValue() : null;
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
