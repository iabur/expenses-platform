package com.expenses.svcexpense;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.expenses.svcexpense", "com.expenses.common" })
public class SvcExpenseApplication {
    public static void main(String[] args) {
        SpringApplication.run(SvcExpenseApplication.class, args);
    }
}
