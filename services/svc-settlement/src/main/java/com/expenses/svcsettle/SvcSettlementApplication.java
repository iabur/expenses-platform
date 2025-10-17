package com.expenses.svcsettle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@ComponentScan(basePackages = { "com.expenses.svcsettle", "com.expenses.common" })
public class SvcSettlementApplication {

  public static void main(String[] args) {
    SpringApplication.run(SvcSettlementApplication.class, args);
  }
}
