package com.example.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for KafkaProducerDemo
 * 
 * This test class covers:
 * - Unit tests for KafkaMessageProducerService
 * - Integration tests for MessageController
 * - Error handling scenarios
 * - Validation logic
 * - Callback functionality
 */
@SpringJUnitConfig
class KafkaProducerDemoTest {

    /**
     * Unit tests for KafkaMessageProducerService
     */
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("KafkaMessageProducerService Tests")
    class KafkaMessageProducerServiceTest {

        @Mock
        private KafkaTemplate<String, Object> kafkaTemplate;

        @Mock
        private ListenableFuture<SendResult<String, Object>> future;

        @Mock
        private SendResult<String, Object> sendResult;

        // Note: RecordMetadata is final and cannot be mocked directly
        // We'll create it using constructor or use lenient mocking

        @InjectMocks
        private KafkaProducerDemo.KafkaMessageProducerService kafkaMessageProducerService;

        @BeforeEach
        void setUp() {
            // Setup common mock behavior only when needed
            lenient().when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);
            lenient().when(kafkaTemplate.send(anyString(), isNull(), any())).thenReturn(future);
            lenient().doNothing().when(future).addCallback(any(ListenableFutureCallback.class));
        }

        @Test
        @DisplayName("Should send message successfully with valid parameters")
        void testSendMessage_Success() {
            // Given
            String topic = "test-topic";
            String key = "test-key";
            String message = "test-message";

            // When
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            // Then
            verify(kafkaTemplate, times(1)).send(topic, key, message);
            verify(future, times(1)).addCallback(any(ListenableFutureCallback.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when topic is null")
        void testSendMessage_NullTopic_ThrowsException() {
            // Given
            String topic = null;
            String key = "test-key";
            String message = "test-message";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Topic name cannot be null or empty", exception.getMessage());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when topic is empty")
        void testSendMessage_EmptyTopic_ThrowsException() {
            // Given
            String topic = "";
            String key = "test-key";
            String message = "test-message";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Topic name cannot be null or empty", exception.getMessage());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when topic contains whitespace only")
        void testSendMessage_WhitespaceOnlyTopic_ThrowsException() {
            // Given
            String topic = "   ";
            String key = "test-key";
            String message = "test-message";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Topic name cannot be null or empty", exception.getMessage());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when message is null")
        void testSendMessage_NullMessage_ThrowsException() {
            // Given
            String topic = "test-topic";
            String key = "test-key";
            Object message = null;

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Message cannot be null", exception.getMessage());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException for invalid topic name characters")
        void testSendMessage_InvalidTopicName_ThrowsException() {
            // Given
            String topic = "invalid@topic";
            String key = "test-key";
            String message = "test-message";

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Topic name contains invalid characters. Only alphanumeric, dots, underscores, and hyphens are allowed.", 
                        exception.getMessage());
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should throw RuntimeException when KafkaTemplate throws exception")
        void testSendMessage_KafkaTemplateThrowsException() {
            // Given
            String topic = "test-topic";
            String key = "test-key";
            String message = "test-message";
            
            when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection failed"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            assertEquals("Failed to send message to Kafka", exception.getMessage());
            verify(kafkaTemplate, times(1)).send(topic, key, message);
        }

        @Test
        @DisplayName("Should send string message with null key")
        void testSendStringMessage() {
            // Given
            String topic = "test-topic";
            String message = "test-message";

            // When
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendStringMessage(topic, message);
            });

            // Then
            verify(kafkaTemplate, times(1)).send(topic, null, message);
            verify(future, times(1)).addCallback(any(ListenableFutureCallback.class));
        }

        @Test
        @DisplayName("Should send message with timestamp key")
        void testSendMessageWithTimestampKey() {
            // Given
            String topic = "test-topic";
            String message = "test-message";

            // When
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendMessageWithTimestampKey(topic, message);
            });

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate, times(1)).send(eq(topic), keyCaptor.capture(), eq(message));
            
            // Verify that the key is a timestamp (numeric string)
            String capturedKey = keyCaptor.getValue();
            assertNotNull(capturedKey);
            assertTrue(capturedKey.matches("\\d+"));
            verify(future, times(1)).addCallback(any(ListenableFutureCallback.class));
        }

        @Test
        @DisplayName("Should return true when Kafka is available")
        void testIsKafkaAvailable_WhenTemplateIsAvailable() {
            // When
            boolean result = kafkaMessageProducerService.isKafkaAvailable();

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("Should return false when KafkaTemplate is null")
        void testIsKafkaAvailable_WhenTemplateIsNull() {
            // Given
            KafkaProducerDemo.KafkaMessageProducerService serviceWithNullTemplate = 
                new KafkaProducerDemo.KafkaMessageProducerService();

            // When
            boolean result = serviceWithNullTemplate.isKafkaAvailable();

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("Should accept valid topic names")
        void testValidTopicNames() {
            // Test valid topic names
            String[] validTopics = {
                "test-topic",
                "test_topic",
                "test.topic",
                "TestTopic123",
                "topic-with-numbers-123",
                "topic_with_underscores",
                "topic.with.dots",
                "a",
                "123",
                "topic-123_test.name"
            };

            for (String topic : validTopics) {
                assertDoesNotThrow(() -> {
                    kafkaMessageProducerService.sendMessage(topic, "key", "message");
                }, "Topic should be valid: " + topic);
            }
        }

        @Test
        @DisplayName("Should reject invalid topic names")
        void testInvalidTopicNames() {
            // Test invalid topic names
            String[] invalidTopics = {
                "topic@invalid",
                "topic with spaces",
                "topic#hash",
                "topic$dollar",
                "topic%percent",
                "topic&ampersand",
                "topic*asterisk",
                "topic+plus",
                "topic=equals",
                "topic[bracket",
                "topic]bracket",
                "topic{brace",
                "topic}brace",
                "topic|pipe",
                "topic\\backslash",
                "topic:colon",
                "topic;semicolon",
                "topic\"quote",
                "topic'apostrophe",
                "topic<less",
                "topic>greater",
                "topic,comma",
                "topic?question",
                "topic/slash"
            };

            for (String topic : invalidTopics) {
                assertThrows(IllegalArgumentException.class, () -> {
                    kafkaMessageProducerService.sendMessage(topic, "key", "message");
                }, "Topic should be invalid: " + topic);
            }
        }

        @Test
        @DisplayName("Should handle null key gracefully")
        void testSendMessage_NullKey_Success() {
            // Given
            String topic = "test-topic";
            String key = null;
            String message = "test-message";

            // When
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendMessage(topic, key, message);
            });

            // Then
            verify(kafkaTemplate, times(1)).send(topic, key, message);
            verify(future, times(1)).addCallback(any(ListenableFutureCallback.class));
        }

        @Test
        @DisplayName("Should handle different message types")
        void testSendMessage_DifferentMessageTypes() {
            // Test with different message types
            Object[] messages = {
                "String message",
                123,
                123.45,
                true,
                new HashMap<String, Object>() {{
                    put("key", "value");
                    put("number", 42);
                }},
                new String[]{"array", "of", "strings"}
            };

            for (Object message : messages) {
                assertDoesNotThrow(() -> {
                    kafkaMessageProducerService.sendMessage("test-topic", "key", message);
                }, "Should handle message type: " + message.getClass().getSimpleName());
            }

            verify(kafkaTemplate, times(messages.length)).send(anyString(), anyString(), any());
        }
    }

    /**
     * Integration tests for MessageController
     */
    @Nested
    @WebMvcTest(KafkaProducerDemo.MessageController.class)
    @DisplayName("MessageController Integration Tests")
    class MessageControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KafkaProducerDemo.KafkaMessageProducerService kafkaProducerService;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("Should send string message successfully")
        void testSendStringMessage_Success() throws Exception {
            // Given
            String topic = "test-topic";
            String message = "Hello World";

            doNothing().when(kafkaProducerService).sendStringMessage(topic, message);

            // When & Then
            mockMvc.perform(post("/api/kafka/send/string")
                    .param("topic", topic)
                    .param("message", message)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Message queued for sending"))
                    .andExpect(jsonPath("$.topic").value(topic))
                    .andExpect(jsonPath("$.payload").value(message));

            verify(kafkaProducerService, times(1)).sendStringMessage(topic, message);
        }

        @Test
        @DisplayName("Should handle string message error")
        void testSendStringMessage_Error() throws Exception {
            // Given
            String topic = "invalid@topic";
            String message = "Hello World";

            doThrow(new IllegalArgumentException("Invalid topic name"))
                .when(kafkaProducerService).sendStringMessage(topic, message);

            // When & Then
            mockMvc.perform(post("/api/kafka/send/string")
                    .param("topic", topic)
                    .param("message", message)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid topic name"))
                    .andExpect(jsonPath("$.topic").value(topic));

            verify(kafkaProducerService, times(1)).sendStringMessage(topic, message);
        }

        @Test
        @DisplayName("Should send keyed message successfully")
        void testSendKeyedMessage_Success() throws Exception {
            // Given
            String topic = "test-topic";
            String key = "test-key";
            String message = "Hello World";

            doNothing().when(kafkaProducerService).sendMessage(topic, key, message);

            // When & Then
            mockMvc.perform(post("/api/kafka/send/keyed")
                    .param("topic", topic)
                    .param("key", key)
                    .param("message", message)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Message queued for sending"))
                    .andExpect(jsonPath("$.topic").value(topic))
                    .andExpect(jsonPath("$.key").value(key))
                    .andExpect(jsonPath("$.payload").value(message));

            verify(kafkaProducerService, times(1)).sendMessage(topic, key, message);
        }

        @Test
        @DisplayName("Should handle keyed message error")
        void testSendKeyedMessage_Error() throws Exception {
            // Given
            String topic = "test-topic";
            String key = "test-key";
            String message = "Hello World";

            doThrow(new RuntimeException("Kafka connection failed"))
                .when(kafkaProducerService).sendMessage(topic, key, message);

            // When & Then
            mockMvc.perform(post("/api/kafka/send/keyed")
                    .param("topic", topic)
                    .param("key", key)
                    .param("message", message)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Kafka connection failed"))
                    .andExpect(jsonPath("$.topic").value(topic))
                    .andExpect(jsonPath("$.key").value(key));

            verify(kafkaProducerService, times(1)).sendMessage(topic, key, message);
        }

        @Test
        @DisplayName("Should send JSON message successfully")
        void testSendJsonMessage_Success() throws Exception {
            // Given
            String topic = "test-topic";
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", 123);
            payload.put("action", "login");
            payload.put("timestamp", "2024-01-15T10:30:00Z");

            doNothing().when(kafkaProducerService).sendMessageWithTimestampKey(eq(topic), any(Map.class));

            // When & Then
            mockMvc.perform(post("/api/kafka/send/json")
                    .param("topic", topic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("JSON message queued for sending"))
                    .andExpect(jsonPath("$.topic").value(topic))
                    .andExpect(jsonPath("$.payload.userId").value(123))
                    .andExpect(jsonPath("$.payload.action").value("login"));

            verify(kafkaProducerService, times(1)).sendMessageWithTimestampKey(eq(topic), any(Map.class));
        }

        @Test
        @DisplayName("Should handle JSON message error")
        void testSendJsonMessage_Error() throws Exception {
            // Given
            String topic = "test-topic";
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", 123);

            doThrow(new IllegalArgumentException("Invalid message format"))
                .when(kafkaProducerService).sendMessageWithTimestampKey(eq(topic), any(Map.class));

            // When & Then
            mockMvc.perform(post("/api/kafka/send/json")
                    .param("topic", topic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Invalid message format"))
                    .andExpect(jsonPath("$.topic").value(topic));

            verify(kafkaProducerService, times(1)).sendMessageWithTimestampKey(eq(topic), any(Map.class));
        }

        @Test
        @DisplayName("Should return health check when service is healthy")
        void testHealthCheck_Healthy() throws Exception {
            // Given
            when(kafkaProducerService.isKafkaAvailable()).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/kafka/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("healthy"))
                    .andExpect(jsonPath("$.kafka").value("connected"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(kafkaProducerService, times(1)).isKafkaAvailable();
        }

        @Test
        @DisplayName("Should return health check when service is unhealthy")
        void testHealthCheck_Unhealthy() throws Exception {
            // Given
            when(kafkaProducerService.isKafkaAvailable()).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/kafka/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("unhealthy"))
                    .andExpect(jsonPath("$.kafka").value("disconnected"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(kafkaProducerService, times(1)).isKafkaAvailable();
        }

        @Test
        @DisplayName("Should handle health check error")
        void testHealthCheck_Error() throws Exception {
            // Given
            when(kafkaProducerService.isKafkaAvailable())
                .thenThrow(new RuntimeException("Health check failed"));

            // When & Then
            mockMvc.perform(get("/api/kafka/health")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Health check failed"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(kafkaProducerService, times(1)).isKafkaAvailable();
        }

        @Test
        @DisplayName("Should return API help documentation")
        void testGetHelp() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/kafka/help")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.service").value("Kafka Producer Demo API"))
                    .andExpect(jsonPath("$.version").value("1.0"))
                    .andExpect(jsonPath("$.endpoints").exists())
                    .andExpect(jsonPath("$.endpoints['POST /api/kafka/send/string']").exists())
                    .andExpect(jsonPath("$.endpoints['POST /api/kafka/send/keyed']").exists())
                    .andExpect(jsonPath("$.endpoints['POST /api/kafka/send/json']").exists())
                    .andExpect(jsonPath("$.endpoints['GET /api/kafka/health']").exists())
                    .andExpect(jsonPath("$.endpoints['GET /api/kafka/help']").exists());
        }

        @Test
        @DisplayName("Should handle missing required parameters")
        void testSendStringMessage_MissingParameters() throws Exception {
            // When & Then - Missing topic parameter
            mockMvc.perform(post("/api/kafka/send/string")
                    .param("message", "Hello World")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            // When & Then - Missing message parameter
            mockMvc.perform(post("/api/kafka/send/string")
                    .param("topic", "test-topic")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing required parameters for keyed message")
        void testSendKeyedMessage_MissingParameters() throws Exception {
            // When & Then - Missing key parameter
            mockMvc.perform(post("/api/kafka/send/keyed")
                    .param("topic", "test-topic")
                    .param("message", "Hello World")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty JSON payload")
        void testSendJsonMessage_EmptyPayload() throws Exception {
            // Given
            String topic = "test-topic";
            Map<String, Object> emptyPayload = new HashMap<>();

            doNothing().when(kafkaProducerService).sendMessageWithTimestampKey(eq(topic), any(Map.class));

            // When & Then
            mockMvc.perform(post("/api/kafka/send/json")
                    .param("topic", topic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(emptyPayload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(kafkaProducerService, times(1)).sendMessageWithTimestampKey(eq(topic), any(Map.class));
        }
    }

    /**
     * Configuration tests
     */
    @Nested
    @SpringBootTest(classes = KafkaProducerDemo.class)
    @DisplayName("Configuration Tests")
    class ConfigurationTest {

        @Autowired
        private KafkaProducerDemo.KafkaProducerConfig kafkaProducerConfig;

        @Test
        @DisplayName("Should create producer factory bean")
        void testProducerFactoryBean() {
            // When
            var producerFactory = kafkaProducerConfig.producerFactory();

            // Then
            assertNotNull(producerFactory);
            assertTrue(producerFactory instanceof org.springframework.kafka.core.DefaultKafkaProducerFactory);
        }

        @Test
        @DisplayName("Should create kafka template bean")
        void testKafkaTemplateBean() {
            // When
            var kafkaTemplate = kafkaProducerConfig.kafkaTemplate();

            // Then
            assertNotNull(kafkaTemplate);
            assertTrue(kafkaTemplate instanceof org.springframework.kafka.core.KafkaTemplate);
        }
    }

    /**
     * Application context tests
     */
    @Nested
    @SpringBootTest(classes = KafkaProducerDemo.class)
    @DisplayName("Application Context Tests")
    class ApplicationContextTest {

        @Autowired
        private KafkaProducerDemo.KafkaMessageProducerService kafkaMessageProducerService;

        @Autowired
        private KafkaProducerDemo.MessageController messageController;

        @Test
        @DisplayName("Should load application context successfully")
        void testApplicationContextLoads() {
            // Then
            assertNotNull(kafkaMessageProducerService);
            assertNotNull(messageController);
        }

        @Test
        @DisplayName("Should have proper service initialization")
        void testServiceInitialization() {
            // Then
            assertTrue(kafkaMessageProducerService.isKafkaAvailable());
        }
    }
}
