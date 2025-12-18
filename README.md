Resilient Message Processing System

This project implements a fault-tolerant message-driven service using Spring Boot, RabbitMQ, and Redis. It demonstrates core distributed-system patterns to ensure exactly-once message processing, even in the presence of failures or duplicate deliveries.

🚀 Features

Exactly-once processing using Redis-based idempotency

Automatic retries for transient failures

Dead-Letter Queue (DLQ) for poison messages

Manual acknowledgements for precise message control

Fault isolation and recovery

🛠 Tech Stack

Java 17

Spring Boot

RabbitMQ

Redis

Docker & Docker Compose

🧠 How It Works

A command message is published to RabbitMQ

The consumer first claims the message ID in Redis

If already processed, the message is ignored

On failure, the message is retried with a retry counter

After max retries, the message is sent to the DLQ

Successful messages are acknowledged and marked as processed

▶️ Running the Project
Start infrastructure
docker-compose up -d

Verify services
docker ps

Check Redis
docker exec -it redis redis-cli ping

Check RabbitMQ

Management UI: http://localhost:15672

Username: guest

Password: guest

🧪 Testing Scenarios

Send duplicate messages → processed only once

Simulate failure → automatic retry

Exceed retry limit → message moves to DLQ

📂 Project Structure
producer-service/
consumer-service/
  ├── listener/
  ├── service/
  ├── config/
docker-compose.yml

🎯 Learning Outcomes

Build resilient consumers

Handle retries and poison messages safely

Apply idempotency in distributed systems

Design production-ready messaging flows
