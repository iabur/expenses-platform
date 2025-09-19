package com.expenses.svcgroup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.expenses.svcgroup", "com.expenses.common" })
public class SvcGroupApplication {
    public static void main(String[] args) {
        SpringApplication.run(SvcGroupApplication.class, args);
    }
}
