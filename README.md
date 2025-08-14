# Kafka Producer with Error Handling Demo

A Spring Boot application demonstrating a robust Kafka producer implementation with comprehensive error handling, asynchronous processing, and detailed logging.

## Features

- **Asynchronous Message Publishing**: Uses Spring's KafkaTemplate with ListenableFuture callbacks
- **Comprehensive Error Handling**: Implements retry logic with exponential backoff
- **Detailed Logging**: Success and failure scenarios with metadata logging
- **REST API**: Easy-to-use endpoints for testing different message types
- **Health Checks**: Built-in health monitoring for Kafka connectivity
- **Production Ready**: Follows Spring Boot best practices

## Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Kafka (for testing with real Kafka cluster)
- Docker (optional, for running Kafka locally)

## Quick Start

### 1. Clone and Build

```bash
git clone <repository-url>
cd kafka-producer-demo
mvn clean compile
```

### 2. Start Kafka (Option A: Docker)

```bash
# Start Kafka using Docker Compose
docker-compose up -d

# Or use individual Docker commands
docker run -d --name zookeeper -p 2181:2181 confluentinc/cp-zookeeper:latest
docker run -d --name kafka -p 9092:9092 --link zookeeper \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka:latest
```

### 2. Start Kafka (Option B: Local Installation)

```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka Server
bin/kafka-server-start.sh config/server.properties

# Create test topic
bin/kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## API Endpoints

### Health Check

```bash
GET http://localhost:8080/api/kafka/health
```

### Send String Message

```bash
POST http://localhost:8080/api/kafka/send/string?topic=test-topic&message=Hello%20World
```

### Send Keyed Message

```bash
POST http://localhost:8080/api/kafka/send/keyed?topic=test-topic&key=user123&message=User%20event
```

### Send JSON Message

```bash
POST http://localhost:8080/api/kafka/send/json?topic=test-topic
Content-Type: application/json

{
  "userId": 123,
  "action": "login",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### Send Bulk Messages

```bash
POST http://localhost:8080/api/kafka/send/bulk?topic=test-topic&count=10
```

### API Help

```bash
GET http://localhost:8080/api/kafka/help
```

## Testing the Error Handling

### 1. Test with Invalid Topic Name

```bash
POST http://localhost:8080/api/kafka/send/string?topic=invalid@topic&message=test
```

### 2. Test with Kafka Down

Stop Kafka and try sending messages to see retry logic in action.

### 3. Monitor Logs

Watch the application logs to see:

- Successful message delivery with metadata
- Retry attempts with exponential backoff
- Error handling and permanent failure scenarios

## Configuration

### Application Properties (`application.yml`)

Key configurations:

- **Kafka Bootstrap Servers**: `spring.kafka.bootstrap-servers`
- **Producer Settings**: Acks, retries, batch size, etc.
- **Logging Levels**: Adjust for different components

### Retry Configuration

In `KafkaMessageProducerService.java`:

```java
private static final int MAX_RETRY_ATTEMPTS = 3;
private static final long INITIAL_RETRY_DELAY_MS = 1000L;
private static final double RETRY_BACKOFF_MULTIPLIER = 2.0;
```

## Monitoring and Observability

### Actuator Endpoints

- Health: `http://localhost:8080/actuator/health`
- Info: `http://localhost:8080/actuator/info`
- Metrics: `http://localhost:8080/actuator/metrics`

### Log Levels

- `com.example.kafka`: DEBUG (application logs)
- `org.springframework.kafka`: INFO (Spring Kafka logs)
- `org.apache.kafka`: WARN (Kafka client logs)

## Testing with Kafka Consumer

### Create a Simple Consumer

```bash
# Console consumer to see messages
bin/kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server localhost:9092
```

### Consumer with Key Display

```bash
bin/kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server localhost:9092 \
  --property print.key=true --property key.separator=":"
```

## Example Usage Scenarios

### 1. Basic String Message

```bash
curl -X POST "http://localhost:8080/api/kafka/send/string?topic=test-topic&message=Hello%20Kafka"
```

### 2. User Event with Key

```bash
curl -X POST "http://localhost:8080/api/kafka/send/keyed?topic=user-events&key=user123&message=User%20logged%20in"
```

### 3. Complex JSON Event

```bash
curl -X POST "http://localhost:8080/api/kafka/send/json?topic=order-events" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-001",
    "customerId": "CUST-123",
    "amount": 99.99,
    "status": "confirmed",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  REST Controller │───▶│  Producer Service │───▶│  Kafka Cluster  │
│                 │    │                  │    │                 │
│ - String msgs   │    │ - Validation     │    │ - Topics        │
│ - Keyed msgs    │    │ - Async sending  │    │ - Partitions    │
│ - JSON msgs     │    │ - Error handling │    │ - Replication   │
│ - Bulk msgs     │    │ - Retry logic    │    │                 │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌──────────────────┐
                       │   Callbacks      │
                       │                  │
                       │ - Success logs   │
                       │ - Failure logs   │
                       │ - Retry attempts │
                       │ - Metrics        │
                       └──────────────────┘
```

## Key Components

### KafkaMessageProducerService

- Main service class with `@Service` annotation
- Implements asynchronous message sending
- Comprehensive error handling with callbacks
- Retry mechanism with exponential backoff
- Input validation and logging

### KafkaProducerConfig

- Configuration for Kafka producer
- Serializers setup (String for keys, JSON for values)
- Performance and reliability settings
- Connection properties

### MessageController

- REST endpoints for testing
- Different message types support
- Health check and help endpoints
- Error handling and response formatting

## Troubleshooting

### Common Issues

1. **Connection Refused**

   - Ensure Kafka is running on localhost:9092
   - Check firewall settings

2. **Topic Not Found**

   - Create topic manually or enable auto-creation
   - Verify topic name spelling

3. **Serialization Errors**

   - Check message format
   - Verify JSON structure for JSON endpoints

4. **Memory Issues**
   - Adjust JVM heap size: `-Xmx512m`
   - Monitor buffer memory settings

### Debug Mode

Enable debug logging:

```yaml
logging:
  level:
    com.example.kafka: DEBUG
    org.springframework.kafka: DEBUG
```

## Production Considerations

1. **Security**: Add SSL/SASL authentication
2. **Monitoring**: Integrate with Prometheus/Grafana
3. **Dead Letter Queue**: Implement for permanent failures
4. **Transactions**: Add transactional support if needed
5. **Schema Registry**: For Avro serialization
6. **Load Testing**: Test with high message volumes

## License

This project is for demonstration purposes and follows the task requirements for implementing a robust Kafka producer with error handling.
