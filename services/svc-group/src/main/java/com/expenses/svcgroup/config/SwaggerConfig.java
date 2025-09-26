package com.expenses.svcgroup.config;

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
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class SwaggerConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";
  private static final String SERVICE_NAME = "Group Service";

  @Bean
  public GroupedOpenApi groupApi() {
    return GroupedOpenApi.builder()
        .group("groups")
        .pathsToMatch("/groups/**")
        .displayName("Group Management")
        .build();
  }

  @Bean
  public OpenAPI groupServiceOpenAPI() {
    Schema<?> errorSchema = new ObjectSchema()
        .addProperty("timestamp", new StringSchema().example("2025-09-27T10:15:30Z"))
        .addProperty("path", new StringSchema().example("/groups"))
        .addProperty("status", new StringSchema().example("403"))
        .addProperty("error", new StringSchema().example("Forbidden"))
        .addProperty("message", new StringSchema().example("User lacks OWNER role"))
        .addProperty("traceId", new StringSchema().example("trace-xyz"))
        .addProperty("requestId", new StringSchema().example("req-67890"))
        .description("Standard error format");

    return new OpenAPI()
        .info(new Info().title(SERVICE_NAME).version("1.0.0")
            .description("Group lifecycle management: creation, membership, roles, and metadata.")
            .contact(new Contact().name("Expenses Platform Team").email("support@expenses-platform.com"))
            .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .addServersItem(new Server().url("http://localhost:8082").description("Local Group Service"))
        .addServersItem(new Server().url("http://svc-group:8082").description("Docker Internal"))
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
            new io.swagger.v3.oas.models.security.SecurityScheme().type(Type.HTTP).scheme("bearer")
                .bearerFormat("JWT")).addSchemas("ErrorResponse", errorSchema))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      Content errorContent = new Content().addMediaType("application/json",
          new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
      operation.getResponses()
          .addApiResponse("400", new ApiResponse().description("Bad Request").content(errorContent))
          .addApiResponse("401", new ApiResponse().description("Unauthorized").content(errorContent))
          .addApiResponse("403", new ApiResponse().description("Forbidden").content(errorContent))
          .addApiResponse("404", new ApiResponse().description("Not Found").content(errorContent))
          .addApiResponse("409", new ApiResponse().description("Conflict - duplicate or invalid state").content(errorContent))
          .addApiResponse("500", new ApiResponse().description("Internal server error").content(errorContent));
    }));
  }

  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter().in("header").name("X-Request-Id")
          .description("Optional request correlation ID").required(false).schema(new StringSchema().example("req-12345-67890")));
      operation.addParametersItem(new Parameter().in("header").name("X-Idempotency-Key")
          .description("Idempotency key for safely retrying POST/PUT").required(false)
          .schema(new StringSchema().example("d7d3f7bb-3db9-4c73-9f42-4ad7c1c4e9c1")));
      return operation;
    };
  }
}
