package com.expenses.svcuser.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger/OpenAPI configuration for User Service.
 * 
 * Configures:
 * - API documentation with proper metadata
 * - JWT Bearer authentication scheme
 * - Global response codes and headers
 * - Server URLs for different environments
 */
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token obtained from Keycloak authentication")
public class SwaggerConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";
  private static final String SERVICE_NAME = "User Service";
  private static final String SERVICE_VERSION = "1.0.0";
  private static final String SERVICE_DESCRIPTION = "User management service for the Expenses Platform";

  /**
   * Configures the API group for user-related endpoints.
   */
  @Bean
  public GroupedOpenApi userApi() {
    return GroupedOpenApi.builder()
        .group("users")
        .pathsToMatch("/user/**")
        .displayName("User Management")
        .build();
  }

  /**
   * Adds global response codes to all API operations.
   */
  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      operation.getResponses()
          .addApiResponse("401", new ApiResponse()
              .description("Unauthorized - missing or invalid JWT token"))
          .addApiResponse("403", new ApiResponse()
              .description("Forbidden - insufficient privileges"))
          .addApiResponse("500", new ApiResponse()
              .description("Internal server error"));
    }));
  }

  /**
   * Configures the main OpenAPI specification with metadata and security.
   */
  @Bean
  public OpenAPI userServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title(SERVICE_NAME)
            .version(SERVICE_VERSION)
            .description(SERVICE_DESCRIPTION)
            .contact(new Contact()
                .name("Expenses Platform Team")
                .email("support@expenses-platform.com"))
            .license(new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT")))
        .addServersItem(new Server()
            .url("http://localhost:8081")
            .description("Local Development Server"))
        .addServersItem(new Server()
            .url("http://svc-user:8081")
            .description("Docker Internal Network"))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME,
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Keycloak")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  /**
   * Adds global headers to all API operations.
   */
  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter()
          .in("header")
          .name("X-Request-Id")
          .description("Optional request correlation ID for tracing")
          .required(false)
          .schema(new StringSchema()
              .example("req-12345-67890")));
      return operation;
    };
  }
}
