# Quick Start Guide - Kafka Producer Demo

This guide will help you quickly set up and test the Kafka Producer with Error Handling demo.

## Prerequisites Check

✅ **Java 11+**: `java -version`  
✅ **Maven 3.6+**: `mvn -version`  
✅ **Docker** (optional): `docker --version`  
✅ **curl**: `curl --version`

## Option 1: Quick Test with Docker (Recommended)

### 1. Start Kafka with Docker

```bash
# Start Kafka and Zookeeper
docker-compose up -d

# Wait for services to be ready (about 30 seconds)
docker-compose logs kafka | grep "started"
```

### 2. Build and Run the Application

```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

### 3. Test the Application

```bash
# Windows
test-kafka-producer.bat all

# Linux/Mac
./test-kafka-producer.sh all
```

## Option 2: Manual Setup

### 1. Download and Start Kafka

```bash
# Download Kafka
wget https://downloads.apache.org/kafka/2.13-3.6.0/kafka_2.13-3.6.0.tgz
tar -xzf kafka_2.13-3.6.0.tgz
cd kafka_2.13-3.6.0

# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka (in another terminal)
bin/kafka-server-start.sh config/server.properties

# Create test topic
bin/kafka-topics.sh --create --topic test-topic --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 2. Run the Application

```bash
mvn spring-boot:run
```

## Quick Tests

### 1. Health Check

```bash
curl http://localhost:8080/api/kafka/health
```

### 2. Send Simple Message

```bash
curl -X POST "http://localhost:8080/api/kafka/send/string?topic=test-topic&message=Hello%20World"
```

### 3. Send JSON Message

```bash
curl -X POST "http://localhost:8080/api/kafka/send/json?topic=test-topic" \
  -H "Content-Type: application/json" \
  -d '{"userId": 123, "action": "login", "timestamp": "2024-01-15T10:30:00Z"}'
```

### 4. Send Bulk Messages

```bash
curl -X POST "http://localhost:8080/api/kafka/send/bulk?topic=test-topic&count=5"
```

## Monitor Messages

### Console Consumer

```bash
# In Kafka directory
bin/kafka-console-consumer.sh --topic test-topic --from-beginning --bootstrap-server localhost:9092 --property print.key=true --property key.separator=":"
```

### Kafka UI (if using Docker)

Open http://localhost:8090 in your browser

## View Application Logs

Watch the application console for detailed logs showing:

- ✅ Successful message delivery with metadata
- 🔄 Retry attempts with exponential backoff
- ❌ Error handling and permanent failures

## Test Error Scenarios

### 1. Invalid Topic Name

```bash
curl -X POST "http://localhost:8080/api/kafka/send/string?topic=invalid@topic&message=test"
```

### 2. Kafka Down

Stop Kafka and try sending messages to see retry logic:

```bash
docker-compose stop kafka
curl -X POST "http://localhost:8080/api/kafka/send/string?topic=test-topic&message=test"
docker-compose start kafka
```

## API Endpoints Summary

| Endpoint                 | Method | Description            |
| ------------------------ | ------ | ---------------------- |
| `/api/kafka/health`      | GET    | Health check           |
| `/api/kafka/send/string` | POST   | Send string message    |
| `/api/kafka/send/keyed`  | POST   | Send message with key  |
| `/api/kafka/send/json`   | POST   | Send JSON message      |
| `/api/kafka/send/bulk`   | POST   | Send multiple messages |
| `/api/kafka/help`        | GET    | API documentation      |

## Troubleshooting

### Application Won't Start

- Check if port 8080 is available
- Verify Java version (11+)
- Check Maven dependencies: `mvn dependency:resolve`

### Can't Connect to Kafka

- Verify Kafka is running: `docker-compose ps`
- Check Kafka logs: `docker-compose logs kafka`
- Ensure port 9092 is not blocked

### No Messages in Consumer

- Check topic exists: `docker exec kafka kafka-topics --list --bootstrap-server localhost:9092`
- Verify application logs for send confirmations
- Check consumer is reading from beginning

### Test Scripts Don't Work

- Ensure curl is installed
- Check if application is running on port 8080
- Verify network connectivity

## Next Steps

1. **Explore the Code**: Check `KafkaMessageProducerService.java` for implementation details
2. **Run Unit Tests**: `mvn test`
3. **Monitor with Actuator**: http://localhost:8080/actuator/health
4. **Customize Configuration**: Edit `application.yml`
5. **Add More Features**: Implement Dead Letter Queue, metrics, etc.

## Clean Up

```bash
# Stop application (Ctrl+C)
# Stop Docker services
docker-compose down

# Remove Docker volumes (optional)
docker-compose down -v
```

Happy testing! 🚀
