package com.expenses.svcexpense.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Feign clients
 */
@Configuration
@EnableFeignClients(basePackages = "com.expenses.svcexpense.client")
public class FeignConfig {
  // Feign configuration can be added here if needed
}
