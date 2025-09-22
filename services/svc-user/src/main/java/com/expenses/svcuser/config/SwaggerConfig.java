package com.expenses.svcuser.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {

  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public GroupedOpenApi userApi() {
    return GroupedOpenApi.builder()
        .group("users")
        .pathsToMatch("/user/**")
        .build();
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      operation.getResponses()
          .addApiResponse("401", new ApiResponse().description("Unauthorized - missing/invalid JWT"))
          .addApiResponse("403", new ApiResponse().description("Forbidden - insufficient privileges"))
          .addApiResponse("500", new ApiResponse().description("Internal server error"));
    }));
  }

  @Bean
  public OpenAPI userServiceOpenAPI() {
    return new OpenAPI()
        .addServersItem(new Server().url("http://localhost:8081").description("Local User Service"))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME,
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter()
          .in("header")
          .name("X-Request-Id")
          .description("Optional request correlation ID")
          .required(false)
          .schema(new StringSchema()));
      return operation;
    };
  }
}
