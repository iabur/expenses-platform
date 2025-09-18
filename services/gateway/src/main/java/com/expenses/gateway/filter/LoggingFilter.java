package com.expenses.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    // Log incoming request
    log.info("Gateway Request: {} {} from {}",
        request.getMethod(),
        request.getURI().getPath(),
        getClientIp(request));

    // Record start time
    long startTime = System.currentTimeMillis();

    return chain.filter(exchange).then(Mono.fromRunnable(() -> {
      ServerHttpResponse response = exchange.getResponse();
      long duration = System.currentTimeMillis() - startTime;

      // Log response
      log.info("Gateway Response: {} {} -> {} ({}ms)",
          request.getMethod(),
          request.getURI().getPath(),
          response.getStatusCode(),
          duration);
    }));
  }

  private String getClientIp(ServerHttpRequest request) {
    String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeaders().getFirst("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";
  }

  @Override
  public int getOrder() {
    return -1; // Execute before other filters
  }
}
