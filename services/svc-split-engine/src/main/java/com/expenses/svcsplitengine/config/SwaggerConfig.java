package com.expenses.svcsplitengine.config;

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
  private static final String SERVICE_NAME = "Split Engine Service";

  @Bean
  public GroupedOpenApi splitEngineApi() {
    return GroupedOpenApi.builder()
        .group("split-engine")
        .pathsToMatch("/api/splits/**")
        .displayName("Split Calculations")
        .build();
  }

  @Bean
  public OpenAPI splitEngineOpenAPI() {
    Schema<?> errorSchema = new ObjectSchema()
        .addProperty("timestamp", new StringSchema().example("2025-09-27T10:15:30Z"))
        .addProperty("path", new StringSchema().example("/api/splits/group/{groupId}/balances"))
        .addProperty("status", new StringSchema().example("404"))
        .addProperty("error", new StringSchema().example("Not Found"))
        .addProperty("message", new StringSchema().example("Group not found"))
        .addProperty("traceId", new StringSchema().example("trace-split-1"))
        .addProperty("requestId", new StringSchema().example("req-split-123"))
        .description("Standard error format");

    return new OpenAPI()
        .info(new Info().title(SERVICE_NAME).version("1.0.0")
            .description("Real-time expense split computation service consuming expense events and producing balances.")
            .contact(new Contact().name("Expenses Platform Team").email("support@expenses-platform.com"))
            .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")))
        .addServersItem(new Server().url("http://localhost:8084").description("Local Split Engine"))
        .addServersItem(new Server().url("http://svc-split-engine:8084").description("Docker Internal"))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME, new io.swagger.v3.oas.models.security.SecurityScheme()
                .type(Type.HTTP).scheme("bearer").bearerFormat("JWT"))
            .addSchemas("ErrorResponse", errorSchema))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values()
        .forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
          Content error = new Content().addMediaType("application/json", new MediaType()
              .schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")));
          operation.getResponses()
              .addApiResponse("400", new ApiResponse().description("Bad Request - invalid parameters").content(error))
              .addApiResponse("401", new ApiResponse().description("Unauthorized - missing/invalid JWT").content(error))
              .addApiResponse("403", new ApiResponse().description("Forbidden - insufficient privileges").content(error))
              .addApiResponse("404", new ApiResponse().description("Not Found").content(error))
              .addApiResponse("500", new ApiResponse().description("Internal server error").content(error));
        }));
  }

  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter().in("header").name("X-Request-Id")
          .description("Optional request correlation ID").required(false)
          .schema(new StringSchema().example("req-split-12345")));
      return operation;
    };
  }
}
