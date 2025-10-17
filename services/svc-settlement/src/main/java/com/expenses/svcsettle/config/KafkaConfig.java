package com.expenses.svcsettle.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.expenses.common.event.DomainEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka configuration for Settlement Service.
 * Configures Kafka producer for publishing settlement events.
 */
@Configuration
@Slf4j
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  /**
   * Configure Kafka producer factory for domain events
   */
  @Bean
  public ProducerFactory<String, DomainEvent> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.ACKS_CONFIG, "all");
    configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
    configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

    log.info("Kafka producer factory configured with bootstrap servers: {}", bootstrapServers);

    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Create Kafka template for publishing domain events
   */
  @Bean
  public KafkaTemplate<String, DomainEvent> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
