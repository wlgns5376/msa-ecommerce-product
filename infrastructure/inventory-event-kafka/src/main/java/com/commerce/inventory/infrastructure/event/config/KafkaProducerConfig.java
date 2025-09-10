package com.commerce.inventory.infrastructure.event.config;

import com.commerce.inventory.infrastructure.event.serialization.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer 설정
 */
@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.producer.acks:all}")
    private String acks;
    
    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;
    
    @Value("${spring.kafka.producer.batch-size:16384}")
    private Integer batchSize;
    
    @Value("${spring.kafka.producer.linger-ms:1}")
    private Integer lingerMs;
    
    @Value("${spring.kafka.producer.buffer-memory:33554432}")
    private Integer bufferMemory;
    
    @Value("${spring.kafka.producer.compression-type:snappy}")
    private String compressionType;
    
    @Value("${spring.kafka.producer.idempotence:true}")
    private Boolean idempotence;
    
    @Bean
    public ProducerFactory<String, EventMessage> producerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configs.put(ProducerConfig.ACKS_CONFIG, acks);
        configs.put(ProducerConfig.RETRIES_CONFIG, retries);
        configs.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        configs.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        configs.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        configs.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
        
        // Request timeout 설정
        configs.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000);
        
        // Idempotent producer를 위한 설정
        if (idempotence) {
            configs.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
            configs.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        }
        
        return new DefaultKafkaProducerFactory<>(configs);
    }
    
    @Bean
    public KafkaTemplate<String, EventMessage> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}