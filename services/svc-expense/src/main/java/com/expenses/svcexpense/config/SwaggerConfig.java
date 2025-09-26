package com.expenses.svcexpense.config;

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

@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
    description = "Paste the JWT access token obtained from Keycloak 'Authorization: Bearer <token>'")
public class SwaggerConfig {
  private static final String SECURITY_SCHEME_NAME = "bearerAuth";
  private static final String SERVICE_NAME = "Expense Service";
  private static final String SERVICE_VERSION = "1.0.0";
  private static final String SERVICE_DESCRIPTION = "Expense recording and participant split service (creates domain events consumed by Split Engine & Ledger).";

  @Bean
  public GroupedOpenApi expensesApi() {
    return GroupedOpenApi.builder()
        .group("expenses")
        .pathsToMatch("/api/expenses/**")
        .displayName("Expense Management")
        .build();
  }

  @Bean
  public OpenAPI expenseServiceOpenAPI() {
    // Reusable Error schema
    Schema<?> errorSchema = new ObjectSchema()
        .addProperty("timestamp", new StringSchema().example("2025-09-27T10:15:30Z"))
        .addProperty("path", new StringSchema().example("/api/expenses"))
        .addProperty("status", new StringSchema().example("400"))
        .addProperty("error", new StringSchema().example("Bad Request"))
        .addProperty("message", new StringSchema().example("Validation failed for field 'amount'") )
        .addProperty("traceId", new StringSchema().example("abc123-trace"))
        .addProperty("requestId", new StringSchema().example("req-12345-67890"))
        .description("Standard error format returned by the Expenses Platform");

    return new OpenAPI()
        .info(new Info()
            .title(SERVICE_NAME)
            .version(SERVICE_VERSION)
            .description(SERVICE_DESCRIPTION)
            .contact(new Contact().name("Expenses Platform Team").email("support@expenses-platform.com"))
            .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .addServersItem(new Server().url("http://localhost:8083").description("Local Development"))
        .addServersItem(new Server().url("http://svc-expense:8083").description("Docker Internal Network"))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME, new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT"))
            .addSchemas("ErrorResponse", errorSchema))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      Content errorContent = new Content().addMediaType("application/json",
          new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
      operation.getResponses()
          .addApiResponse("400", new ApiResponse().description("Bad Request - validation failure").content(errorContent))
          .addApiResponse("401", new ApiResponse().description("Unauthorized - missing/invalid JWT").content(errorContent))
          .addApiResponse("403", new ApiResponse().description("Forbidden - insufficient privileges").content(errorContent))
          .addApiResponse("404", new ApiResponse().description("Not Found").content(errorContent))
          .addApiResponse("409", new ApiResponse().description("Conflict - duplicate or invalid state").content(errorContent))
          .addApiResponse("500", new ApiResponse().description("Internal server error").content(errorContent));
    }));
  }

  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter().in("header").name("X-Request-Id")
          .description("Optional request correlation ID").required(false)
          .schema(new StringSchema().example("req-12345-67890")));
      operation.addParametersItem(new Parameter().in("header").name("X-Idempotency-Key")
          .description("Provide for idempotent POST/PUT to safely retry (UUID recommended)").required(false)
          .schema(new StringSchema().example("1e0c0e54-9a6b-4b62-8f7c-9d9e6f5d2a10")));
      return operation;
    };
  }
}
