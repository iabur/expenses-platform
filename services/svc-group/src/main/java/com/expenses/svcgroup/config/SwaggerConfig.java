package com.expenses.svcgroup.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

@Configuration
public class SwaggerConfig {
  private static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Bean
  public GroupedOpenApi groupsApi() {
    return GroupedOpenApi.builder()
        .group("groups")
        .pathsToMatch("/groups/**", "/members/**")
        .build();
  }

  @Bean
  public OpenApiCustomizer globalResponsesCustomiser() {
    return openApi -> openApi.getPaths().values()
        .forEach(pathItem -> pathItem.readOperations().forEach(operation -> operation.getResponses()
            .addApiResponse("401", new ApiResponse().description("Unauthorized - missing/invalid JWT"))
            .addApiResponse("403", new ApiResponse().description("Forbidden - insufficient privileges"))
            .addApiResponse("500", new ApiResponse().description("Internal server error"))));
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
