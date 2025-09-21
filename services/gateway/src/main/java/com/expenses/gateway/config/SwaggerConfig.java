package com.expenses.gateway.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

@Configuration
public class SwaggerConfig {

        private static final String SECURITY_SCHEME_NAME = "bearerAuth";

        @Bean
        @Primary
        public GroupedOpenApi gatewayApi() {
                return GroupedOpenApi.builder()
                                .group("gateway")
                                .pathsToMatch("/api/gateway/**")
                                .build();
        }

        @Bean
        public RouterFunction<ServerResponse> swaggerRouterFunction() {
                return RouterFunctions.route()
                                .GET("/internal/api-docs/swagger-config", this::swaggerConfig)
                                .build();
        }

        private Mono<ServerResponse> swaggerConfig(ServerRequest request) {
                Map<String, Object> config = new HashMap<>();
                config.put("configUrl", "/internal/api-docs/swagger-config");

                List<Map<String, String>> urls = Arrays.asList(
                                createSwaggerUrl("users", "User Service"),
                                createSwaggerUrl("groups", "Group Service"),
                                createSwaggerUrl("expenses", "Expense Service"),
                                createSwaggerUrl("ledger", "Ledger Service"),
                                createSwaggerUrl("settlements", "Settlement Service"),
                                createSwaggerUrl("split-engine", "Split Engine Service"),
                                createSwaggerUrl("fx", "FX Service"),
                                Map.of("url", "/internal/api-docs", "name", "Gateway Service"));

                config.put("urls", urls);

                return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(config);
        }

        private Map<String, String> createSwaggerUrl(String serviceName, String displayName) {
                return Map.of(
                                "url", "/v3/api-docs/" + serviceName,
                                "name", displayName);
        }
}
