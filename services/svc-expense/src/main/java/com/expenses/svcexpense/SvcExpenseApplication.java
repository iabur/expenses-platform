package com.expenses.svcexpense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.expenses.svcexpense", "com.expenses.common" })
@EnableFeignClients(basePackages = "com.expenses.svcexpense.client")
public class SvcExpenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(SvcExpenseApplication.class, args);
    }
}
