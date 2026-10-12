# Testing Documentation – FinGuard

## Overview

FinGuard includes comprehensive test suites across backend Java microservices, Python FastAPI machine learning services, and containerized Docker environments.

---

## 1. Backend Java Tests (JUnit 5 & Mockito)

To run the backend test suite:
```bash
cd backend
mvn test
```

### Test Coverage Areas
- **Fraud Rules**: Unit tests verifying threshold triggers for `HighAmountRule`, `RapidTransactionRule`, `LocationMismatchRule`, `VelocityRule`, and `DeviceChangeRule`.
- **Fraud Rule Engine**: Rule aggregation, score capping at 100, and combined alert string generation.
- **Authentication & Security**: BCrypt password hashing, JWT token validation, and role authorization filters.

---

## 2. Python ML Service Tests (Pytest)

To run the ML service test suite:
```bash
cd ml-service
python -m pytest
```

### Test Coverage Areas
- **Health check**: `/health` endpoint verification.
- **Prediction Endpoint**: `/predict` payload validation, feature matrix transformation, and probability output ranges [0.0, 1.0].
- **Model Loading**: Verifying joblib model deserialization from `fraud_model.pkl`.

---

## 3. End-to-End Local Verification (Docker Compose)

```bash
docker compose up --build
```

### Verification Endpoints
- **Spring Boot Actuator Health**: `http://localhost:8080/api/health`
- **FastAPI Health**: `http://localhost:8000/health`
- **Swagger API Documentation**: `http://localhost:8080/swagger-ui.html`
- **React Dashboard**: `http://localhost:3000`
