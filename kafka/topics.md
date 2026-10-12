# Kafka Topics – FinGuard

## Overview

FinGuard uses Apache Kafka as the backbone of its event-driven transaction processing pipeline.

---

## Topics

### `transactions`

| Property | Value |
|---|---|
| Partitions | 3 |
| Replication Factor | 1 (local dev) / 3 (production) |
| Retention | 7 days |
| Cleanup Policy | delete |

**Purpose**: Every new transaction submitted via the REST API is published here. The `TransactionConsumer` subscribes and drives the fraud detection pipeline.

**Message format (JSON)**:
```json
{
  "transactionId": "TXN-20240101-ABCD",
  "userId": 42,
  "amount": 25000.00,
  "currency": "INR",
  "merchant": "Amazon India",
  "location": "Hyderabad",
  "transactionType": "PAYMENT",
  "deviceId": "DEVICE-XYZ-001",
  "timestamp": "2026-07-31T10:00:00"
}
```

---

### `fraud-alerts`

| Property | Value |
|---|---|
| Partitions | 1 |
| Replication Factor | 1 (local dev) / 3 (production) |
| Retention | 30 days |

**Purpose**: When a transaction is flagged SUSPICIOUS or FRAUD, an alert event is published here for downstream consumers (e.g., notification service, audit log).

**Message format (JSON)**:
```json
{
  "alertId": "ALERT-20240101-XYZ",
  "transactionId": "TXN-20240101-ABCD",
  "severity": "HIGH",
  "fraudScore": 82.5,
  "reasons": ["HIGH_AMOUNT", "DEVICE_CHANGE"],
  "timestamp": "2026-07-31T10:00:01"
}
```

---

### `transaction-results`

| Property | Value |
|---|---|
| Partitions | 3 |
| Replication Factor | 1 (local dev) |

**Purpose**: After fraud analysis completes, the final result (fraud status, score, decision) is published for any interested consumers.

---

## Consumer Groups

| Group ID | Topic(s) | Description |
|---|---|---|
| `finguard-transaction-consumer` | `transactions` | Main fraud detection pipeline consumer |
| `finguard-alert-consumer` | `fraud-alerts` | Alert processing / notification consumer |

---

## Local Development

Topics are auto-created by Kafka (`auto.create.topics.enable=true`) in the local Docker Compose setup.

In production (AWS MSK), topics should be pre-created with appropriate replication factors using:

```bash
kafka-topics.sh --create \
  --bootstrap-server <MSK_BROKER>:9092 \
  --replication-factor 3 \
  --partitions 3 \
  --topic transactions
```
