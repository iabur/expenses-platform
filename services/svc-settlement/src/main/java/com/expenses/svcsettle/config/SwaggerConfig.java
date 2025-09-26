package com.expenses.svcsettle.config;

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
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme.Type;
import io.swagger.v3.oas.models.servers.Server;

/**
 * Swagger/OpenAPI configuration for Settlement Service.
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
  private static final String SERVICE_NAME = "Settlement Service";
  private static final String SERVICE_VERSION = "1.0.0";
  private static final String SERVICE_DESCRIPTION = "Settlement management service for the Expenses Platform";

  /**
   * Configures the API group for settlement-related endpoints.
   */
  @Bean
  public GroupedOpenApi settlementsApi() {
    return GroupedOpenApi.builder()
        .group("settlements")
        .pathsToMatch("/api/settlements/**")
        .displayName("Settlement Management")
        .build();
  }

  /**
   * Configures the main OpenAPI specification with metadata and security.
   */
  @Bean
  public OpenAPI settlementServiceOpenAPI() {
    Schema<?> errorSchema = new ObjectSchema()
        .addProperty("timestamp", new StringSchema().example("2025-09-27T10:15:30Z"))
        .addProperty("path", new StringSchema().example("/api/settlements/proposals"))
        .addProperty("status", new StringSchema().example("400"))
        .addProperty("error", new StringSchema().example("Bad Request"))
        .addProperty("message", new StringSchema().example("Proposal cannot be accepted in current state"))
        .addProperty("traceId", new StringSchema().example("trace-settlement-1"))
        .addProperty("requestId", new StringSchema().example("req-settle-123"))
        .description("Standard error format returned by settlement service operations");

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
            .url("http://localhost:8085")
            .description("Local Development Server"))
        .addServersItem(new Server()
            .url("http://svc-settlement:8085")
            .description("Docker Internal Network"))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME,
                new io.swagger.v3.oas.models.security.SecurityScheme()
                    .type(Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token from Keycloak"))
            .addSchemas("ErrorResponse", errorSchema))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  /**
   * Adds global response codes to all API operations.
   */
  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      Content errorContent = new Content().addMediaType("application/json",
          new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
      operation.getResponses()
          .addApiResponse("400", new ApiResponse()
              .description("Bad Request - validation/business rule failure").content(errorContent))
          .addApiResponse("401", new ApiResponse()
              .description("Unauthorized - missing or invalid JWT token").content(errorContent))
          .addApiResponse("403", new ApiResponse()
              .description("Forbidden - insufficient privileges").content(errorContent))
          .addApiResponse("404", new ApiResponse()
              .description("Not Found - resource does not exist").content(errorContent))
          .addApiResponse("409", new ApiResponse()
              .description("Conflict - state transition not allowed").content(errorContent))
          .addApiResponse("500", new ApiResponse()
              .description("Internal server error").content(errorContent));
    }));
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
      operation.addParametersItem(new Parameter()
          .in("header")
          .name("X-Idempotency-Key")
          .description("Optional idempotency key for proposal creation (UUID recommended)")
          .required(false)
          .schema(new StringSchema().example("0f4c1b1e-9d6a-4e1c-8b2d-1234567890ab")));
      return operation;
    };
  }
}
