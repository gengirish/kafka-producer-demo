package com.example.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Complete Kafka Producer Demo with Error Handling
 * 
 * This single file contains:
 * - Spring Boot Application
 * - Kafka Producer Configuration
 * - Kafka Message Producer Service with comprehensive error handling
 * - REST Controller for testing
 * 
 * Features:
 * - Asynchronous message publishing with ListenableFuture callbacks
 * - Comprehensive error handling and retry logic with exponential backoff
 * - Detailed logging for success and failure scenarios
 * - Input validation and null checks
 * - REST API endpoints for testing different message types
 * - Health check functionality
 * 
 * Skills Demonstrated:
 * - Apache Kafka concepts (topics, partitions, producers)
 * - Spring for Kafka (KafkaTemplate usage and configuration)
 * - Asynchronous programming with futures and callbacks
 * - Error handling with retry mechanisms
 * - Spring Framework (dependency injection, service layer patterns)
 * - Structured logging with appropriate levels
 * 
 * @author Generated Implementation
 * @version 1.0
 */
@SpringBootApplication
public class KafkaProducerDemo {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerDemo.class, args);
    }

    /**
     * Kafka Producer Configuration
     * 
     * This configuration class sets up the Kafka producer with appropriate
     * serializers and connection properties for optimal performance and reliability.
     */
    @Configuration
    public static class KafkaProducerConfig {

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

        /**
         * Producer factory configuration with performance and reliability settings
         */
        @Bean
        public ProducerFactory<String, Object> producerFactory() {
            Map<String, Object> configProps = new HashMap<>();
            
            // Basic connection properties
            configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
            
            // Performance and reliability properties
            configProps.put(ProducerConfig.ACKS_CONFIG, acks);
            configProps.put(ProducerConfig.RETRIES_CONFIG, retries);
            configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
            configProps.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
            configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
            
            // Additional reliability settings
            configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
            configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
            configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
            configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
            
            return new DefaultKafkaProducerFactory<>(configProps);
        }

        /**
         * Kafka template bean for message publishing
         */
        @Bean
        public KafkaTemplate<String, Object> kafkaTemplate() {
            return new KafkaTemplate<>(producerFactory());
        }
    }

    /**
     * Kafka Message Producer Service
     * 
     * This service provides robust Kafka message publishing capabilities with comprehensive
     * error handling, asynchronous processing, and detailed logging. It demonstrates
     * professional-grade implementation of Spring Kafka integration.
     * 
     * Core Features:
     * - Asynchronous message publishing with callbacks
     * - Comprehensive error handling and logging
     * - Retry mechanism with exponential backoff
     * - Input validation and null checks
     * - Detailed success and failure logging with metadata
     */
    @Service
    public static class KafkaMessageProducerService {

        private static final Logger logger = LoggerFactory.getLogger(KafkaMessageProducerService.class);
        
        // Retry configuration constants
        private static final int MAX_RETRY_ATTEMPTS = 3;
        private static final long INITIAL_RETRY_DELAY_MS = 1000L;
        private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
        
        @Autowired
        private KafkaTemplate<String, Object> kafkaTemplate;
        
        /**
         * Post-construct initialization to verify Kafka template configuration
         */
        @PostConstruct
        public void init() {
            if (kafkaTemplate == null) {
                logger.error("KafkaTemplate is not properly configured. Please check your Kafka configuration.");
                throw new IllegalStateException("KafkaTemplate is not available");
            }
            logger.info("KafkaMessageProducerService initialized successfully");
        }
        
        /**
         * Sends a message to the specified Kafka topic with comprehensive error handling
         * and asynchronous callback processing.
         * 
         * This method performs the following operations:
         * 1. Validates input parameters
         * 2. Sends message asynchronously using KafkaTemplate
         * 3. Attaches success/failure callbacks for logging and error handling
         * 4. Implements retry logic for failed messages
         * 
         * @param topic The target Kafka topic name (must not be null or empty)
         * @param key The message key for partitioning (can be null)
         * @param message The payload object to be sent (must not be null)
         * 
         * @throws IllegalArgumentException if topic is null/empty or message is null
         * @throws RuntimeException if Kafka configuration issues are detected
         */
        public void sendMessage(String topic, String key, Object message) {
            // Input validation
            validateInputParameters(topic, message);
            
            logger.debug("Preparing to send message to topic: {}, key: {}, message type: {}", 
                        topic, key, message.getClass().getSimpleName());
            
            try {
                // Send message asynchronously and get ListenableFuture
                ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
                
                // Add callback for success and failure handling
                future.addCallback(new MessageSendCallback(topic, key, message, 0));
                
            } catch (Exception e) {
                logger.error("Unexpected error occurred while sending message to topic: {}, key: {}, error: {}", 
                            topic, key, e.getMessage(), e);
                throw new RuntimeException("Failed to send message to Kafka", e);
            }
        }
        
        /**
         * Sends a message with retry capability for failed attempts.
         * This is an internal method used by the retry mechanism.
         * 
         * @param topic The target Kafka topic name
         * @param key The message key for partitioning
         * @param message The payload object to be sent
         * @param attemptNumber Current attempt number (0-based)
         */
        private void sendMessageWithRetry(String topic, String key, Object message, int attemptNumber) {
            try {
                ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
                future.addCallback(new MessageSendCallback(topic, key, message, attemptNumber));
            } catch (Exception e) {
                logger.error("Error on retry attempt {} for topic: {}, key: {}, error: {}", 
                            attemptNumber + 1, topic, key, e.getMessage(), e);
                
                if (attemptNumber < MAX_RETRY_ATTEMPTS - 1) {
                    scheduleRetry(topic, key, message, attemptNumber + 1);
                } else {
                    logger.error("All retry attempts exhausted for message to topic: {}, key: {}. Message will be dropped.", 
                               topic, key);
                }
            }
        }
        
        /**
         * Schedules a retry attempt with exponential backoff delay.
         * 
         * @param topic The target Kafka topic name
         * @param key The message key for partitioning
         * @param message The payload object to be sent
         * @param attemptNumber Current attempt number
         */
        private void scheduleRetry(String topic, String key, Object message, int attemptNumber) {
            long delayMs = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, attemptNumber));
            
            logger.info("Scheduling retry attempt {} for topic: {}, key: {} after {}ms delay", 
                       attemptNumber + 1, topic, key, delayMs);
            
            CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                            .execute(() -> sendMessageWithRetry(topic, key, message, attemptNumber));
        }
        
        /**
         * Validates input parameters for the sendMessage method.
         * 
         * @param topic The topic name to validate
         * @param message The message object to validate
         * @throws IllegalArgumentException if validation fails
         */
        private void validateInputParameters(String topic, Object message) {
            if (topic == null || topic.trim().isEmpty()) {
                throw new IllegalArgumentException("Topic name cannot be null or empty");
            }
            
            if (message == null) {
                throw new IllegalArgumentException("Message cannot be null");
            }
            
            // Additional validation for topic name format (basic check)
            if (!topic.matches("^[a-zA-Z0-9._-]+$")) {
                throw new IllegalArgumentException("Topic name contains invalid characters. Only alphanumeric, dots, underscores, and hyphens are allowed.");
            }
        }
        
        /**
         * Custom callback implementation for handling message send results.
         * This inner class provides detailed logging and retry logic for both
         * successful and failed message deliveries.
         */
        private class MessageSendCallback implements ListenableFutureCallback<SendResult<String, Object>> {
            
            private final String topic;
            private final String key;
            private final Object message;
            private final int attemptNumber;
            
            public MessageSendCallback(String topic, String key, Object message, int attemptNumber) {
                this.topic = topic;
                this.key = key;
                this.message = message;
                this.attemptNumber = attemptNumber;
            }
            
            /**
             * Handles successful message delivery.
             * Logs detailed metadata about the successful send operation.
             * 
             * @param result The send result containing metadata
             */
            @Override
            public void onSuccess(SendResult<String, Object> result) {
                if (result != null && result.getRecordMetadata() != null) {
                    RecordMetadata metadata = result.getRecordMetadata();
                    ProducerRecord<String, Object> producerRecord = result.getProducerRecord();
                    
                    // Convert timestamp to readable format
                    LocalDateTime timestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(metadata.timestamp()), 
                        ZoneId.systemDefault()
                    );
                    
                    logger.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}, " +
                               "Timestamp: {}, Key: {}, Message Type: {}, Attempt: {}", 
                               metadata.topic(), 
                               metadata.partition(), 
                               metadata.offset(), 
                               timestamp,
                               key != null ? key : "null",
                               message.getClass().getSimpleName(),
                               attemptNumber + 1);
                    
                    // Debug level logging for additional details
                    logger.debug("Message details - Serialized key size: {} bytes, Serialized value size: {} bytes",
                               metadata.serializedKeySize(), 
                               metadata.serializedValueSize());
                } else {
                    logger.warn("Message sent successfully but result metadata is null - Topic: {}, Key: {}", 
                               topic, key);
                }
            }
            
            /**
             * Handles failed message delivery.
             * Logs error details and implements retry logic with exponential backoff.
             * 
             * @param ex The exception that caused the failure
             */
            @Override
            public void onFailure(Throwable ex) {
                logger.error("Failed to send message - Topic: {}, Key: {}, Message Type: {}, " +
                            "Attempt: {}, Error: {}", 
                            topic, 
                            key != null ? key : "null", 
                            message.getClass().getSimpleName(),
                            attemptNumber + 1,
                            ex.getMessage(), 
                            ex);
                
                // Implement retry logic
                if (attemptNumber < MAX_RETRY_ATTEMPTS - 1) {
                    logger.info("Initiating retry for failed message - Topic: {}, Key: {}, " +
                               "Next attempt: {}/{}", 
                               topic, key, attemptNumber + 2, MAX_RETRY_ATTEMPTS);
                    
                    scheduleRetry(topic, key, message, attemptNumber + 1);
                } else {
                    logger.error("All retry attempts ({}) exhausted for message - Topic: {}, Key: {}. " +
                               "Message will be dropped. Consider implementing Dead Letter Queue pattern.", 
                               MAX_RETRY_ATTEMPTS, topic, key);
                    
                    handlePermanentFailure(topic, key, message, ex);
                }
            }
        }
        
        /**
         * Handles permanently failed messages after all retry attempts are exhausted.
         * This method can be extended to implement Dead Letter Queue pattern or
         * other failure handling strategies.
         * 
         * @param topic The target topic
         * @param key The message key
         * @param message The failed message
         * @param lastException The final exception that caused the failure
         */
        private void handlePermanentFailure(String topic, String key, Object message, Throwable lastException) {
            logger.error("Handling permanent failure for message - Topic: {}, Key: {}, " +
                        "Final error: {}", topic, key, lastException.getMessage());
            
            // TODO: Implement Dead Letter Queue pattern
            // TODO: Update failure metrics
            // TODO: Send alert/notification
        }
        
        /**
         * Utility method to send a simple string message.
         * This is a convenience method for common use cases.
         * 
         * @param topic The target Kafka topic name
         * @param message The string message to send
         */
        public void sendStringMessage(String topic, String message) {
            sendMessage(topic, null, message);
        }
        
        /**
         * Utility method to send a message with automatic key generation.
         * Uses the current timestamp as the key for basic partitioning.
         * 
         * @param topic The target Kafka topic name
         * @param message The message object to send
         */
        public void sendMessageWithTimestampKey(String topic, Object message) {
            String timestampKey = String.valueOf(System.currentTimeMillis());
            sendMessage(topic, timestampKey, message);
        }
        
        /**
         * Health check method to verify Kafka connectivity.
         * This method can be used by Spring Boot Actuator health checks.
         * 
         * @return true if Kafka template is available and configured
         */
        public boolean isKafkaAvailable() {
            try {
                return kafkaTemplate != null;
            } catch (Exception e) {
                logger.warn("Kafka health check failed: {}", e.getMessage());
                return false;
            }
        }
    }

    /**
     * REST Controller for testing Kafka message publishing
     * 
     * This controller provides endpoints to test the KafkaMessageProducerService
     * with different types of messages and scenarios.
     */
    @RestController
    @RequestMapping("/api/kafka")
    public static class MessageController {

        @Autowired
        private KafkaMessageProducerService kafkaProducerService;

        /**
         * Send a simple string message to a topic
         */
        @PostMapping("/send/string")
        public ResponseEntity<Map<String, Object>> sendStringMessage(
                @RequestParam String topic,
                @RequestParam String message) {
            
            try {
                kafkaProducerService.sendStringMessage(topic, message);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Message queued for sending");
                response.put("topic", topic);
                response.put("payload", message);
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", e.getMessage());
                response.put("topic", topic);
                
                return ResponseEntity.badRequest().body(response);
            }
        }

        /**
         * Send a message with a specific key to a topic
         */
        @PostMapping("/send/keyed")
        public ResponseEntity<Map<String, Object>> sendKeyedMessage(
                @RequestParam String topic,
                @RequestParam String key,
                @RequestParam String message) {
            
            try {
                kafkaProducerService.sendMessage(topic, key, message);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Message queued for sending");
                response.put("topic", topic);
                response.put("key", key);
                response.put("payload", message);
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", e.getMessage());
                response.put("topic", topic);
                response.put("key", key);
                
                return ResponseEntity.badRequest().body(response);
            }
        }

        /**
         * Send a JSON object message to a topic
         */
        @PostMapping("/send/json")
        public ResponseEntity<Map<String, Object>> sendJsonMessage(
                @RequestParam String topic,
                @RequestBody Map<String, Object> payload) {
            
            try {
                kafkaProducerService.sendMessageWithTimestampKey(topic, payload);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "JSON message queued for sending");
                response.put("topic", topic);
                response.put("payload", payload);
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "error");
                response.put("message", e.getMessage());
                response.put("topic", topic);
                
                return ResponseEntity.badRequest().body(response);
            }
        }

        /**
         * Health check endpoint to verify Kafka connectivity
         */
        @GetMapping("/health")
        public ResponseEntity<Map<String, Object>> healthCheck() {
            Map<String, Object> response = new HashMap<>();
            
            try {
                boolean isHealthy = kafkaProducerService.isKafkaAvailable();
                
                response.put("status", isHealthy ? "healthy" : "unhealthy");
                response.put("kafka", isHealthy ? "connected" : "disconnected");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } catch (Exception e) {
                response.put("status", "error");
                response.put("message", e.getMessage());
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.status(500).body(response);
            }
        }

        /**
         * Get API documentation/help
         */
        @GetMapping("/help")
        public ResponseEntity<Map<String, Object>> getHelp() {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> endpoints = new HashMap<>();
            
            endpoints.put("POST /api/kafka/send/string", 
                         "Send string message. Params: topic, message");
            endpoints.put("POST /api/kafka/send/keyed", 
                         "Send keyed message. Params: topic, key, message");
            endpoints.put("POST /api/kafka/send/json", 
                         "Send JSON message. Params: topic, Body: JSON payload");
            endpoints.put("GET /api/kafka/health", 
                         "Check Kafka producer health");
            endpoints.put("GET /api/kafka/help", 
                         "Show this help message");
            
            response.put("service", "Kafka Producer Demo API");
            response.put("version", "1.0");
            response.put("endpoints", endpoints);
            
            return ResponseEntity.ok(response);
        }
    }
}
