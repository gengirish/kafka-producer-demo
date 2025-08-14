#!/bin/bash

# Kafka Producer Test Script
# This script provides easy commands to test the Kafka producer service

BASE_URL="http://localhost:8080/api/kafka"

echo "=== Kafka Producer Test Script ==="
echo "Make sure the Spring Boot application is running on port 8080"
echo ""

# Function to check if the service is running
check_service() {
    echo "Checking if service is running..."
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/health")
    if [ "$response" = "200" ]; then
        echo "✅ Service is running"
        return 0
    else
        echo "❌ Service is not running or not responding"
        return 1
    fi
}

# Function to send a string message
send_string_message() {
    local topic=${1:-"test-topic"}
    local message=${2:-"Hello from test script"}
    
    echo "Sending string message to topic: $topic"
    curl -X POST "$BASE_URL/send/string?topic=$topic&message=$message" \
         -H "Content-Type: application/json" | jq '.'
    echo ""
}

# Function to send a keyed message
send_keyed_message() {
    local topic=${1:-"test-topic"}
    local key=${2:-"test-key"}
    local message=${3:-"Keyed message from test script"}
    
    echo "Sending keyed message to topic: $topic with key: $key"
    curl -X POST "$BASE_URL/send/keyed?topic=$topic&key=$key&message=$message" \
         -H "Content-Type: application/json" | jq '.'
    echo ""
}

# Function to send a JSON message
send_json_message() {
    local topic=${1:-"test-topic"}
    
    echo "Sending JSON message to topic: $topic"
    curl -X POST "$BASE_URL/send/json?topic=$topic" \
         -H "Content-Type: application/json" \
         -d '{
           "userId": 12345,
           "action": "test_action",
           "timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%SZ)'",
           "metadata": {
             "source": "test-script",
             "version": "1.0"
           }
         }' | jq '.'
    echo ""
}

# Function to send bulk messages
send_bulk_messages() {
    local topic=${1:-"test-topic"}
    local count=${2:-5}
    
    echo "Sending $count bulk messages to topic: $topic"
    curl -X POST "$BASE_URL/send/bulk?topic=$topic&count=$count" \
         -H "Content-Type: application/json" | jq '.'
    echo ""
}

# Function to test error handling
test_error_handling() {
    echo "Testing error handling with invalid topic name..."
    curl -X POST "$BASE_URL/send/string?topic=invalid@topic&message=test" \
         -H "Content-Type: application/json" | jq '.'
    echo ""
}

# Function to show help
show_help() {
    echo "Available commands:"
    echo "  health                          - Check service health"
    echo "  string [topic] [message]        - Send string message"
    echo "  keyed [topic] [key] [message]   - Send keyed message"
    echo "  json [topic]                    - Send JSON message"
    echo "  bulk [topic] [count]            - Send bulk messages"
    echo "  error                           - Test error handling"
    echo "  all                             - Run all tests"
    echo "  help                            - Show this help"
    echo ""
    echo "Examples:"
    echo "  ./test-kafka-producer.sh health"
    echo "  ./test-kafka-producer.sh string user-events 'User logged in'"
    echo "  ./test-kafka-producer.sh keyed order-events order123 'Order created'"
    echo "  ./test-kafka-producer.sh bulk test-topic 10"
}

# Function to run all tests
run_all_tests() {
    echo "Running all tests..."
    echo ""
    
    check_service || exit 1
    
    echo "1. Testing string message..."
    send_string_message "test-topic" "Test string message"
    
    echo "2. Testing keyed message..."
    send_keyed_message "test-topic" "test-key-123" "Test keyed message"
    
    echo "3. Testing JSON message..."
    send_json_message "test-topic"
    
    echo "4. Testing bulk messages..."
    send_bulk_messages "test-topic" 3
    
    echo "5. Testing error handling..."
    test_error_handling
    
    echo "All tests completed!"
}

# Main script logic
case "${1:-help}" in
    "health")
        check_service
        ;;
    "string")
        send_string_message "$2" "$3"
        ;;
    "keyed")
        send_keyed_message "$2" "$3" "$4"
        ;;
    "json")
        send_json_message "$2"
        ;;
    "bulk")
        send_bulk_messages "$2" "$3"
        ;;
    "error")
        test_error_handling
        ;;
    "all")
        run_all_tests
        ;;
    "help"|*)
        show_help
        ;;
esac
