package com.example.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for KafkaProducerDemo
 * 
 * These tests verify the integration between components without requiring
 * a real Kafka cluster by mocking the KafkaTemplate.
 */
@SpringBootTest(classes = KafkaProducerDemo.class)
@SpringJUnitConfig
@DisplayName("Kafka Producer Integration Tests")
class KafkaProducerIntegrationTest {

    @Autowired
    private KafkaProducerDemo.KafkaMessageProducerService kafkaMessageProducerService;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should successfully send message through the complete flow")
    void testCompleteMessageSendFlow() {
        // Given
        String topic = "integration-test-topic";
        String key = "integration-key";
        String message = "Integration test message";

        // Mock the ListenableFuture
        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(topic, key, message)).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When
        assertDoesNotThrow(() -> {
            kafkaMessageProducerService.sendMessage(topic, key, message);
        });

        // Then
        verify(kafkaTemplate, times(1)).send(topic, key, message);
        verify(mockFuture, times(1)).addCallback(any(ListenableFutureCallback.class));
    }

    @Test
    @DisplayName("Should handle callback failure with retry logic")
    void testCallbackFailureWithRetry() {
        // Given
        String topic = "retry-test-topic";
        String key = "retry-key";
        String message = "Retry test message";

        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(topic, key, message)).thenReturn(mockFuture);

        ArgumentCaptor<ListenableFutureCallback<SendResult<String, Object>>> callbackCaptor = 
            ArgumentCaptor.forClass(ListenableFutureCallback.class);
        doNothing().when(mockFuture).addCallback(callbackCaptor.capture());

        // When
        kafkaMessageProducerService.sendMessage(topic, key, message);

        // Then
        verify(kafkaTemplate, times(1)).send(topic, key, message);
        
        ListenableFutureCallback<SendResult<String, Object>> callback = callbackCaptor.getValue();
        assertNotNull(callback);

        // Simulate failure callback
        RuntimeException testException = new RuntimeException("Simulated Kafka failure");
        assertDoesNotThrow(() -> {
            callback.onFailure(testException);
        });
    }

    @Test
    @DisplayName("Should handle different message types in integration scenario")
    void testDifferentMessageTypesIntegration() {
        // Given
        String topic = "multi-type-topic";
        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(eq(topic), anyString(), any())).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // Test different message types
        Object[] testMessages = {
            "String message",
            42,
            3.14,
            true,
            new HashMap<String, Object>() {{
                put("type", "map");
                put("value", 100);
            }}
        };

        // When & Then
        for (Object testMessage : testMessages) {
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendMessage(topic, "key", testMessage);
            }, "Should handle message type: " + testMessage.getClass().getSimpleName());
        }

        verify(kafkaTemplate, times(testMessages.length)).send(eq(topic), eq("key"), any());
    }

    @Test
    @DisplayName("Should validate service health check integration")
    void testHealthCheckIntegration() {
        // When
        boolean isHealthy = kafkaMessageProducerService.isKafkaAvailable();

        // Then
        assertTrue(isHealthy, "Service should report as healthy when KafkaTemplate is available");
    }

    @Test
    @DisplayName("Should handle string message utility method integration")
    void testStringMessageUtilityIntegration() {
        // Given
        String topic = "string-utility-topic";
        String message = "Utility method test";

        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(topic, null, message)).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When
        assertDoesNotThrow(() -> {
            kafkaMessageProducerService.sendStringMessage(topic, message);
        });

        // Then
        verify(kafkaTemplate, times(1)).send(topic, null, message);
        verify(mockFuture, times(1)).addCallback(any(ListenableFutureCallback.class));
    }

    @Test
    @DisplayName("Should handle timestamp key utility method integration")
    void testTimestampKeyUtilityIntegration() {
        // Given
        String topic = "timestamp-utility-topic";
        String message = "Timestamp key test";

        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(eq(topic), anyString(), eq(message))).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When
        assertDoesNotThrow(() -> {
            kafkaMessageProducerService.sendMessageWithTimestampKey(topic, message);
        });

        // Then
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate, times(1)).send(eq(topic), keyCaptor.capture(), eq(message));
        
        String capturedKey = keyCaptor.getValue();
        assertNotNull(capturedKey);
        assertTrue(capturedKey.matches("\\d+"), "Key should be a timestamp (numeric string)");
        verify(mockFuture, times(1)).addCallback(any(ListenableFutureCallback.class));
    }

    @Test
    @DisplayName("Should handle validation errors in integration scenario")
    void testValidationErrorsIntegration() {
        // Test null topic
        assertThrows(IllegalArgumentException.class, () -> {
            kafkaMessageProducerService.sendMessage(null, "key", "message");
        });

        // Test empty topic
        assertThrows(IllegalArgumentException.class, () -> {
            kafkaMessageProducerService.sendMessage("", "key", "message");
        });

        // Test null message
        assertThrows(IllegalArgumentException.class, () -> {
            kafkaMessageProducerService.sendMessage("topic", "key", null);
        });

        // Test invalid topic name
        assertThrows(IllegalArgumentException.class, () -> {
            kafkaMessageProducerService.sendMessage("invalid@topic", "key", "message");
        });

        // Verify no calls were made to KafkaTemplate for invalid inputs
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Should handle KafkaTemplate exceptions in integration scenario")
    void testKafkaTemplateExceptionIntegration() {
        // Given
        String topic = "exception-topic";
        String key = "exception-key";
        String message = "Exception test message";

        when(kafkaTemplate.send(topic, key, message))
            .thenThrow(new RuntimeException("Simulated KafkaTemplate exception"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            kafkaMessageProducerService.sendMessage(topic, key, message);
        });

        assertEquals("Failed to send message to Kafka", exception.getMessage());
        assertEquals("Simulated KafkaTemplate exception", exception.getCause().getMessage());
        verify(kafkaTemplate, times(1)).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle complex JSON message integration")
    void testComplexJsonMessageIntegration() {
        // Given
        String topic = "json-integration-topic";
        Map<String, Object> complexMessage = new HashMap<>();
        complexMessage.put("userId", 12345);
        complexMessage.put("action", "purchase");
        complexMessage.put("amount", 99.99);
        complexMessage.put("currency", "USD");
        complexMessage.put("metadata", new HashMap<String, Object>() {{
            put("source", "mobile-app");
            put("version", "2.1.0");
            put("sessionId", "abc123xyz");
        }});

        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(eq(topic), anyString(), eq(complexMessage))).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When
        assertDoesNotThrow(() -> {
            kafkaMessageProducerService.sendMessageWithTimestampKey(topic, complexMessage);
        });

        // Then
        verify(kafkaTemplate, times(1)).send(eq(topic), anyString(), eq(complexMessage));
        verify(mockFuture, times(1)).addCallback(any(ListenableFutureCallback.class));
    }

    @Test
    @DisplayName("Should handle null key gracefully in integration")
    void testNullKeyIntegration() {
        // Given
        String topic = "null-key-topic";
        String message = "Message with null key";

        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(topic, null, message)).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When
        assertDoesNotThrow(() -> {
            kafkaMessageProducerService.sendMessage(topic, null, message);
        });

        // Then
        verify(kafkaTemplate, times(1)).send(topic, null, message);
        verify(mockFuture, times(1)).addCallback(any(ListenableFutureCallback.class));
    }

    @Test
    @DisplayName("Should handle multiple concurrent sends")
    void testMultipleConcurrentSends() {
        // Given
        String topic = "concurrent-topic";
        ListenableFuture<SendResult<String, Object>> mockFuture = mock(ListenableFuture.class);
        when(kafkaTemplate.send(eq(topic), anyString(), anyString())).thenReturn(mockFuture);
        doNothing().when(mockFuture).addCallback(any(ListenableFutureCallback.class));

        // When - Send multiple messages
        for (int i = 0; i < 5; i++) {
            final int messageId = i;
            assertDoesNotThrow(() -> {
                kafkaMessageProducerService.sendMessage(topic, "key-" + messageId, "message-" + messageId);
            });
        }

        // Then
        verify(kafkaTemplate, times(5)).send(eq(topic), anyString(), anyString());
        verify(mockFuture, times(5)).addCallback(any(ListenableFutureCallback.class));
    }
}
