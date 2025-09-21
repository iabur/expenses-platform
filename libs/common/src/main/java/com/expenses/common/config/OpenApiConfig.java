package com.expenses.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:expenses-platform}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    @Primary
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(getServiceTitle())
                        .description(getServiceDescription())
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Expenses Platform Team")
                                .email("support@expenses-platform.com")
                                .url("https://expenses-platform.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"))
                .addServersItem(new Server()
                        .url("https://api.expenses-platform.com")
                        .description("Production Server"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from Keycloak")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    private String getServiceTitle() {
        return switch (applicationName) {
            case "svc-user" -> "User Service API";
            case "svc-group" -> "Group Management API";
            case "svc-expense" -> "Expense Management API";
            case "svc-settlement" -> "Settlement & Payment API";
            case "svc-split-engine" -> "Split Calculation API";
            case "svc-ledger" -> "Ledger & Balance API";
            case "svc-fx" -> "Foreign Exchange API";
            case "gateway" -> "API Gateway";
            default -> "Expenses Platform API";
        };
    }

    private String getServiceDescription() {
        return switch (applicationName) {
            case "svc-user" -> """
                    User profile and preferences management service.

                    Features:
                    • User profile CRUD operations
                    • Notification preferences
                    • User search functionality
                    • JWT-based authentication
                    """;
            case "svc-group" -> """
                    Group and membership management service.

                    Features:
                    • Group creation and management
                    • Member invitation and role management
                    • Group settings and preferences
                    • Access control and permissions
                    """;
            case "svc-expense" -> """
                    Expense tracking and management service.

                    Features:
                    • Expense CRUD operations
                    • Participant management
                    • Line item support
                    • File attachments
                    • Category-based organization
                    """;
            case "svc-settlement" -> """
                    Settlement proposal and payment tracking service.

                    Features:
                    • Settlement proposal creation and management
                    • Payment confirmation workflow
                    • Dispute resolution
                    • Debt optimization algorithms
                    • Payment method flexibility
                    """;
            case "svc-split-engine" -> """
                    Expense split calculation and balance management service.

                    Features:
                    • Multiple split calculation methods (equal, percentage, exact, shares)
                    • Real-time balance tracking
                    • Debt simplification algorithms
                    • Multi-currency support
                    """;
            case "svc-ledger" -> """
                    Double-entry bookkeeping and financial ledger service.

                    Features:
                    • Double-entry accounting
                    • Journal entries and postings
                    • Account balance tracking
                    • Financial audit trails
                    """;
            case "svc-fx" -> """
                    Foreign exchange rate management service.

                    Features:
                    • Real-time exchange rates
                    • Historical rate data
                    • Multi-currency conversion
                    • Rate change notifications
                    """;
            case "gateway" -> """
                    API Gateway for routing, authentication, and cross-cutting concerns.

                    Features:
                    • Request routing and load balancing
                    • Rate limiting and circuit breaking
                    • CORS handling
                    • Authentication offloading
                    """;
            default -> "Comprehensive expense sharing and settlement platform with microservices architecture.";
        };
    }
}
