@echo off
REM Kafka Producer Test Script for Windows
REM This script provides easy commands to test the Kafka producer service

set BASE_URL=http://localhost:8080/api/kafka

echo === Kafka Producer Test Script ===
echo Make sure the Spring Boot application is running on port 8080
echo.

if "%1"=="" goto help
if "%1"=="help" goto help
if "%1"=="health" goto health
if "%1"=="string" goto string
if "%1"=="keyed" goto keyed
if "%1"=="json" goto json
if "%1"=="bulk" goto bulk
if "%1"=="error" goto error
if "%1"=="all" goto all
goto help

:health
echo Checking if service is running...
curl -s "%BASE_URL%/health"
echo.
goto end

:string
set topic=%2
set message=%3
if "%topic%"=="" set topic=test-topic
if "%message%"=="" set message=Hello from Windows test script

echo Sending string message to topic: %topic%
curl -X POST "%BASE_URL%/send/string?topic=%topic%&message=%message%" -H "Content-Type: application/json"
echo.
goto end

:keyed
set topic=%2
set key=%3
set message=%4
if "%topic%"=="" set topic=test-topic
if "%key%"=="" set key=test-key
if "%message%"=="" set message=Keyed message from Windows test script

echo Sending keyed message to topic: %topic% with key: %key%
curl -X POST "%BASE_URL%/send/keyed?topic=%topic%&key=%key%&message=%message%" -H "Content-Type: application/json"
echo.
goto end

:json
set topic=%2
if "%topic%"=="" set topic=test-topic

echo Sending JSON message to topic: %topic%
curl -X POST "%BASE_URL%/send/json?topic=%topic%" -H "Content-Type: application/json" -d "{\"userId\": 12345, \"action\": \"test_action\", \"timestamp\": \"%date% %time%\", \"metadata\": {\"source\": \"windows-test-script\", \"version\": \"1.0\"}}"
echo.
goto end

:bulk
set topic=%2
set count=%3
if "%topic%"=="" set topic=test-topic
if "%count%"=="" set count=5

echo Sending %count% bulk messages to topic: %topic%
curl -X POST "%BASE_URL%/send/bulk?topic=%topic%&count=%count%" -H "Content-Type: application/json"
echo.
goto end

:error
echo Testing error handling with invalid topic name...
curl -X POST "%BASE_URL%/send/string?topic=invalid@topic&message=test" -H "Content-Type: application/json"
echo.
goto end

:all
echo Running all tests...
echo.

echo 1. Testing health check...
call :health

echo 2. Testing string message...
call :string test-topic "Test string message"

echo 3. Testing keyed message...
call :keyed test-topic test-key-123 "Test keyed message"

echo 4. Testing JSON message...
call :json test-topic

echo 5. Testing bulk messages...
call :bulk test-topic 3

echo 6. Testing error handling...
call :error

echo All tests completed!
goto end

:help
echo Available commands:
echo   health                          - Check service health
echo   string [topic] [message]        - Send string message
echo   keyed [topic] [key] [message]   - Send keyed message
echo   json [topic]                    - Send JSON message
echo   bulk [topic] [count]            - Send bulk messages
echo   error                           - Test error handling
echo   all                             - Run all tests
echo   help                            - Show this help
echo.
echo Examples:
echo   test-kafka-producer.bat health
echo   test-kafka-producer.bat string user-events "User logged in"
echo   test-kafka-producer.bat keyed order-events order123 "Order created"
echo   test-kafka-producer.bat bulk test-topic 10

:end
