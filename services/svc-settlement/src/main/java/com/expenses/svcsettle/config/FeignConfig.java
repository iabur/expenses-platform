package com.expenses.svcsettle.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Feign clients
 */
@Configuration
@EnableFeignClients(basePackages = "com.expenses.svcsettle.client")
public class FeignConfig {
  // Feign configuration can be added here if needed
}
