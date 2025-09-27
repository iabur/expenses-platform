package com.expenses.svcledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ComponentScan(basePackages = { "com.expenses.svcledger", "com.expenses.common" })
public class SvcLedgerApplication {
  public static void main(String[] args) {
    SpringApplication.run(SvcLedgerApplication.class, args);
  }
}
