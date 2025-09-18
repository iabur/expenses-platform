package com.expenses.svcledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class SvcLedgerApplication {
  public static void main(String[] args) {
    SpringApplication.run(SvcLedgerApplication.class, args);
  }
}
