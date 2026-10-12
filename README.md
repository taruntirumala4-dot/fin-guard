# FinGuard – Real-Time Financial Transaction Monitoring & Fraud Detection System

FinGuard is a production-style, event-driven financial transaction monitoring and fraud detection platform built with Java, Spring Boot, Apache Kafka, Redis, PostgreSQL, Python FastAPI, Scikit-learn, and React.

---

## 🌟 Key Features

- **Real-Time Event Processing**: Asynchronous transaction ingestion via Apache Kafka.
- **Hybrid Fraud Detection Engine**:
  - **Rule-Based Engine**: Configurable heuristic rules (`HighAmountRule`, `RapidTransactionRule`, `LocationMismatchRule`, `VelocityRule`, `DeviceChangeRule`).
  - **Machine Learning Microservice**: Random Forest Classifier evaluating tabular features to compute fraud probabilities.
  - **Composite Risk Scoring**: `Final Score = (Rule Score × 0.4) + (ML Score × 0.6)`.
- **High-Performance Caching**: Redis cache-aside pattern for fast lookups and dashboard metric caching.
- **Enterprise Security**: JWT (HMAC-SHA256) authentication, BCrypt password hashing, and Role-Based Access Control (`USER`, `ANALYST`, `ADMIN`).
- **Interactive Executive Dashboard**: Modern React dark-mode dashboard with real-time graphs (Recharts), searchable/filterable audit tables, and transaction simulation modals.
- **Containerized & Cloud-Ready**: Fully runnable with a single command via Docker Compose and pre-configured for AWS ECS, RDS, MSK, ElastiCache deployment.

---

## 🏗️ High-Level Architecture

```
React Dashboard (Port 3000)
       │
       ▼
Spring Boot REST API (Port 8080) ◄─────► Redis Cache (Port 6379)
       │                                     ▲
       ├─────────────────┐                   │ (Fast lookups)
       ▼                 ▼                   │
PostgreSQL (5432)   Kafka Topic 'transactions'
                         │
                         ▼
             Transaction Consumer
                         │
        ┌────────────────┴────────────────┐
        ▼                                 ▼
Fraud Rule Engine                 Python ML Service (FastAPI Port 8000)
(Java 17 Heuristics)              (Random Forest Classifier)
        │                                 │
        └────────────────┬────────────────┘
                         ▼
               Composite Risk Decision
                         │
          ┌──────────────┴──────────────┐
          ▼                             ▼
   Update PostgreSQL            Create Fraud Alert
```

---

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| **Backend** | Java 17, Spring Boot 3.2, Spring Web, Spring Data JPA, Spring Security, JWT, Spring Kafka, Spring Redis |
| **ML Microservice** | Python 3.11, FastAPI, Scikit-Learn, Pandas, NumPy, Joblib, Uvicorn |
| **Frontend** | React 18, Vite, JavaScript, Axios, React Router v6, Recharts, Lucide Icons |
| **Messaging** | Apache Kafka 3.5, Zookeeper |
| **Cache & DB** | Redis 7, PostgreSQL 15 |
| **DevOps** | Docker, Docker Compose, Multi-stage Dockerfiles |
| **AWS Cloud** | ECS Fargate, RDS PostgreSQL, ElastiCache Redis, Amazon MSK, S3 + CloudFront |

---

## 🚀 Quick Start (Docker Compose)

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.

### Launch Full Stack
```bash
# Clone repository
git clone https://github.com/your-username/fin-guard.git
cd fin-guard

# Copy environment variables template
cp .env.example .env

# Start all services with Docker Compose
docker compose up --build
```

### Access Ports & Interfaces
- **React Dashboard**: `http://localhost:3000`
- **Spring Boot REST API**: `http://localhost:8080`
- **Swagger API Specs**: `http://localhost:8080/swagger-ui.html`
- **FastAPI ML Service Health**: `http://localhost:8000/health`

### Demo Login Credentials
- **Admin**: `admin@finguard.io` / `Password@123`
- **Analyst**: `priya.sharma@finguard.io` / `Password@123`
- **User**: `vikram.patel@example.com` / `Password@123`

---

## 📊 Fraud Scoring Strategy

The composite fraud score combines traditional rule-based risk points with machine learning predictive probability:

$$\text{Final Score} = (\text{Rule Score} \times 0.4) + (\text{ML Probability} \times 100 \times 0.6)$$

| Final Score | Fraud Status | Transaction Action | Alert Created? |
|---|---|---|---|
| **0 – 30** | `SAFE` | Approved (`SUCCESS`) | No |
| **31 – 60** | `SUSPICIOUS` | Approved & Flagged (`SUCCESS`) | Yes (`MEDIUM`) |
| **61 – 100** | `FRAUD` | Blocked (`BLOCKED`) | Yes (`HIGH` / `CRITICAL`) |

---

## 🎯 Demo Scenarios

1. **Normal Transaction**: Amount ₹1,500, known device, normal location $\rightarrow$ **`SAFE`**
2. **High Amount Threshold**: Amount ₹250,000 $\rightarrow$ Triggers `HIGH_AMOUNT` rule $\rightarrow$ **`FRAUD`**
3. **Rapid Transactions**: 7 transactions in 2 minutes $\rightarrow$ Triggers `RAPID_TRANSACTION` rule $\rightarrow$ **`SUSPICIOUS`**
4. **Location Mismatch (Impossible Travel)**: Hyderabad at 14:00 $\rightarrow$ Delhi at 14:10 $\rightarrow$ Triggers `LOCATION_MISMATCH` $\rightarrow$ **`FRAUD`**
5. **Device Change**: Transaction from unrecognized device $\rightarrow$ Triggers `DEVICE_CHANGE` $\rightarrow$ **`SUSPICIOUS`**

---

## 📁 Directory Structure

```
fin-guard/
├── docker-compose.yml
├── README.md
├── LICENSE
├── .env.example
├── .gitignore
├── backend/            # Spring Boot REST API & Kafka Pipeline
├── frontend/           # React 18 + Vite Analytics Dashboard
├── ml-service/         # Python FastAPI + Scikit-Learn ML Model
├── database/           # PostgreSQL Schema (DDL) and Seed Data
├── kafka/              # Kafka Topic Specifications
├── docker/             # Production Dockerfiles
├── aws/                # Cloud Architecture & Deployment Guides
├── tests/              # JUnit 5 and Pytest Suites
└── docs/               # Architecture & API Documentation
```

---

## 📝 License

This project is open-source under the [MIT License](LICENSE).
