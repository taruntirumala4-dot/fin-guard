# Database – FinGuard

## Overview

FinGuard uses **PostgreSQL 15** as its primary relational database.

## Tables

### `users`
Stores registered user accounts with BCrypt-hashed passwords and role-based access control.

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL | Primary key |
| name | VARCHAR(100) | Display name |
| email | VARCHAR(255) | Unique, used for login |
| password_hash | VARCHAR(255) | BCrypt hash |
| role | ENUM | USER / ADMIN / ANALYST |
| created_at | TIMESTAMPTZ | Account creation time |

### `transactions`
Core financial transaction records.

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL | Primary key |
| transaction_id | VARCHAR(50) | Unique business ID (e.g. TXN-2026-XXXXX) |
| user_id | BIGINT | FK → users(id) |
| amount | NUMERIC(15,2) | Must be > 0 |
| currency | VARCHAR(10) | Default: INR |
| merchant | VARCHAR(255) | Merchant/payee name |
| location | VARCHAR(255) | Transaction location |
| transaction_type | ENUM | PAYMENT / TRANSFER / WITHDRAWAL / DEPOSIT |
| device_id | VARCHAR(100) | Device identifier |
| timestamp | TIMESTAMPTZ | Transaction occurrence time |
| status | ENUM | SUCCESS / FAILED / PENDING / BLOCKED |
| fraud_score | NUMERIC(5,2) | 0–100 composite score |
| fraud_status | ENUM | SAFE / SUSPICIOUS / FRAUD |
| created_at | TIMESTAMPTZ | Record creation time |

### `fraud_alerts`
Alerts generated for suspicious/fraudulent transactions.

| Column | Type | Notes |
|---|---|---|
| id | BIGSERIAL | Primary key |
| transaction_id | VARCHAR(50) | FK → transactions(transaction_id) |
| alert_type | VARCHAR(100) | HIGH_AMOUNT / RAPID_TRANSACTION / etc |
| severity | ENUM | LOW / MEDIUM / HIGH / CRITICAL |
| reason | TEXT | Human-readable explanation |
| fraud_score | NUMERIC(5,2) | Score at time of alert |
| status | ENUM | OPEN / INVESTIGATING / RESOLVED / FALSE_POSITIVE |
| created_at | TIMESTAMPTZ | Alert creation time |

## Running Locally

```bash
# Via Docker Compose (recommended)
docker compose up postgres

# Manually apply schema and seed
psql -h localhost -U postgres -d finguard -f schema.sql
psql -h localhost -U postgres -d finguard -f seed.sql
```

## Indexes

Key indexes for performance:
- `idx_transactions_user_id` — fast user transaction lookups
- `idx_transactions_timestamp` — time-range queries
- `idx_transactions_fraud_status` — fraud dashboard filters
- `idx_transactions_user_timestamp` — composite for fraud velocity checks
- `idx_fraud_alerts_severity` — alert severity filtering
