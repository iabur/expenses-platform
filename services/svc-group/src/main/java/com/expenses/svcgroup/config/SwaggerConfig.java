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
import io.swagger.v3.oas.models.info.Info;
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

  @Bean
  public GroupedOpenApi groupApi() {
    return GroupedOpenApi.builder()
        .group("groups")
        .pathsToMatch("/groups/**")
        .displayName("Group Management")
        .build();
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
      operation.getResponses()
          .addApiResponse("401", new ApiResponse().description("Unauthorized"))
          .addApiResponse("403", new ApiResponse().description("Forbidden"))
          .addApiResponse("500", new ApiResponse().description("Internal server error"));
    }));
  }

  @Bean
  public OpenAPI groupServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info().title("Group Service").version("1.0.0"))
        .addServersItem(new Server().url("http://localhost:8082").description("Local Group Service"))
        .addServersItem(new Server().url("http://svc-group:8082").description("Docker Internal"))
        .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
            new io.swagger.v3.oas.models.security.SecurityScheme().type(Type.HTTP).scheme("bearer")
                .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  @Bean
  public OperationCustomizer addGlobalHeaders() {
    return (operation, handlerMethod) -> {
      operation.addParametersItem(new Parameter().in("header").name("X-Request-Id")
          .description("Optional request correlation ID").required(false).schema(new StringSchema()));
      return operation;
    };
  }
}
