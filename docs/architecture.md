# Architecture Documentation – FinGuard

## Overview

FinGuard is a high-throughput, event-driven financial transaction monitoring and fraud detection platform.

---

## Technical Stack

- **Backend**: Java 17, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, Spring Kafka, Spring Redis
- **ML Microservice**: Python 3.11, FastAPI, Scikit-Learn (Random Forest), Pandas, Joblib
- **Frontend**: React 18, Vite, Recharts, Lucide Icons, Vanilla CSS
- **Infrastructure**: PostgreSQL 15, Redis 7, Apache Kafka (Confluent 7.5), Zookeeper, Docker Compose
- **Cloud Readiness**: AWS ECS Fargate, RDS PostgreSQL, ElastiCache Redis, Amazon MSK, CloudFront + S3

---

## Data Flow Pipeline

```
[React Frontend]
       │ (1. POST /api/transactions)
       ▼
[Spring Boot REST API]
       │ (2. Persist Initial Txn as PENDING)
       ├─────────────────────────────────┐
       │ (3. Async Kafka Publish)        │ (Fast Read Lookup)
       ▼                                 ▼
[Kafka Topic: 'transactions']     [Redis Cache]
       │
       ▼ (4. TransactionConsumer)
[Fraud Detection Service]
       ├───► [Fraud Rule Engine] (Rule Score: 0–100)
       │         - High Amount Rule
       │         - Rapid Transaction Rule
       │         - Location Mismatch Rule
       │         - Velocity Rule
       │         - Device Change Rule
       │
       ├───► [Python FastAPI ML Service] (POST /predict -> Fraud Probability: 0.0-1.0)
       │         - Random Forest Classifier (trained on tabular features)
       │
       ▼ (5. Composite Score Calculation)
   Final Score = (Rule Score * 0.4) + (ML Score * 0.6)
       │
       ├─► Update Transaction (Status: SUCCESS / BLOCKED, Fraud Status: SAFE / SUSPICIOUS / FRAUD)
       ├─► Generate FraudAlert if SUSPICIOUS/FRAUD
       └─► Invalidate & Update Redis Cache
```

---

## Event-Driven Architecture Advantages

1. **Non-Blocking User Experience**: Financial transactions are accepted immediately with `PENDING` status while fraud detection executes asynchronously in Kafka consumers.
2. **Decoupled Machine Learning**: The Python FastAPI service can scale independently based on inference load without blocking core HTTP thread pools.
3. **Cache-Aside Pattern**: Frequently queried transaction lists and dashboard summaries are cached in Redis with strict TTL policies, preventing database bottlenecking under peak load.
