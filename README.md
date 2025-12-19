# 🚀 Galactic Message Relay – Resilient Message Processing System

A production-style **resilient asynchronous message processing system** built using **Spring Boot, RabbitMQ, Redis, and Docker**.
This project demonstrates **idempotency, retries, poison (dead-letter) queues, failure simulation, and monitoring**, inspired by real-world distributed systems.

---

## 🧩 Architecture Overview

```
Client
  │
  ▼
Producer Service (HTTP API)
  │
  ▼
RabbitMQ (Primary Queue)
  │
  ▼
Consumer Worker
  │   ├── Redis (Idempotency / State Store)
  │   ├── Retry Logic (max 3 retries)
  │   └── Poison Queue (DLQ)
  ▼
RabbitMQ (DLQ)
```

### Services

| Service          | Description                                             | Port            |
| ---------------- | ------------------------------------------------------- | --------------- |
| Producer Service | Accepts messages via REST API and publishes to RabbitMQ | `8080`          |
| Consumer Service | Processes messages with retries & idempotency           | `8081`          |
| RabbitMQ         | Message broker + management UI                          | `5672`, `15672` |
| Redis            | State store for idempotency                             | `6379`          |

---

## ✨ Features Implemented

✔ Exactly-once processing using Redis-based idempotency
✔ Manual acknowledgements (ACK)
✔ Retry mechanism (3 attempts)
✔ Poison / Dead-Letter Queue for failed messages
✔ Simulated failures (~30%) to test resilience
✔ Monitoring endpoint (`/status`)
✔ Fully Dockerized setup (one command run)

---

## 🐳 Run the Project (Docker – Recommended)

### 1️⃣ Prerequisites

* Docker
* Docker Compose
  Verify:

```bash
docker --version
docker compose version
```

##Pull Infrastructure Images (IMPORTANT)

Explicitly pull Redis and RabbitMQ before composing:
```bash
docker pull rabbitmq:3-management
docker pull redis:7
```

### 2️⃣ Start all services

```bash
docker compose up -d --build
```

### 3️⃣ Verify running containers

```bash
docker ps
```

---

## 🔧 Access Services

### RabbitMQ Management UI

```
http://localhost:15672
username: guest
password: guest
```

### Consumer Status API

```
GET http://localhost:8081/status
```

Response example:

```json
{
  "primaryQueue": 0,
  "poisonQueue": 1
}
```

---

## 📡 Producer API (Message Ingestion)

### Endpoint

```
POST http://localhost:8080/command
```

### Sample Request

```json
{
  "messageId": "cmd-101",
  "payload": {
    "orderId": 123,
    "action": "DEPLOY"
  },
  "createdAt": "2025-01-01T10:00:00Z"
}
```

### Expected Response

```json
{
  "status": "QUEUED",
  "messageId": "cmd-101"
}
```

---

## 🔄 Consumer Processing Flow

1. Consumer listens to **primary queue**
2. Attempts to **claim messageId in Redis**
3. If duplicate → ACK & ignore
4. Simulates processing (30% failure)
5. On failure:

   * Retry up to **3 times**
   * Preserve headers (`x-retries`)
6. After retries exhausted → move to **Poison Queue**
7. On success → store `messageId` in Redis → ACK

---

## ☠️ Testing Failure & Poison Queue

### Option A – Natural Failure (30%)

Send multiple messages; some will fail automatically.

### Option B – Forced Failure (Recommended)

Temporarily modify `ProcessingService`:

```java
throw new SimulatedProcessingException(message.getMessageId());
```

Rebuild & restart:

```bash
docker compose up -d --build consumer-service
```

### Observe logs

```bash
docker logs -f consumer-service
```

Expected:

```
[RETRYING attempt=1]
[RETRYING attempt=2]
[RETRYING attempt=3]
[MOVED_TO_DLQ]
```

---

## 🧪 Verify Poison Queue

### Using RabbitMQ UI

1. Go to **Queues**
2. Open `command.dlq`
3. Click **Get Messages**

Headers visible:

* `x-retries`
* `x-error-reason`
* `x-original-queue`

---

## 🔁 Idempotency Test

Send the **same messageId again**:

```json
{
  "messageId": "cmd-101",
  "payload": {"orderId": 123}
}
```

Consumer log:

```
[DUPLICATE_ALREADY_PROCESSED]
```

✔ Message ignored
✔ No reprocessing

---

## 🛠️ Tech Stack

* **Java 21**
* **Spring Boot**
* **RabbitMQ**
* **Redis**
* **Docker & Docker Compose**

---

## 🎯 Why This Project Matters

This system demonstrates **real-world distributed system patterns**:

* Idempotent consumers
* Fault tolerance
* Backpressure control
* Dead-letter queues
* Stateless microservices

Ideal for **backend interviews**, **system design discussions**, and **production-grade learning**.

---

## 👤 Author

**Chandra Naga Mouli**
Backend / Distributed Systems Enthusiast

---

⭐ If you found this useful, consider starring the repo!
