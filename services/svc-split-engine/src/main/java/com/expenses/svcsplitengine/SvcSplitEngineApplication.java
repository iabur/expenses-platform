package com.expenses.svcsplitengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableKafka
@EnableTransactionManagement
@ComponentScan(basePackages = { "com.expenses.svcsplitengine", "com.expenses.common" })
public class SvcSplitEngineApplication {

  public static void main(String[] args) {
    SpringApplication.run(SvcSplitEngineApplication.class, args);
  }
}
