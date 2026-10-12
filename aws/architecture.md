# AWS Architecture – FinGuard
## Real-Time Transactions & Fraud Detection System

---

## Overview

FinGuard is deployed entirely on AWS. The Spring Boot backend runs on **Amazon EC2**
and receives incoming financial transactions. Each transaction is saved to **Amazon RDS**
(PostgreSQL), then pushed to **Amazon SQS** for async fraud processing. An **AWS Lambda**
function is triggered by SQS, runs the fraud verification logic, calls **Amazon SageMaker**
for an AI-powered fraud probability score, updates RDS, and fires a customer alert via
**Amazon SNS**. Transaction reports and ML training datasets are stored in **Amazon S3**.
**Amazon CloudWatch** monitors the entire system, and **AWS IAM** secures access between
every service with least-privilege roles.

---

## Architecture Diagram

```
Internet Users (Mobile / Web App)
            ¦
            ?
+-----------------------------------+
¦  Amazon S3 + CloudFront (CDN)     ¦
¦  React frontend static assets     ¦
+-----------------------------------+
               ¦
               ?
+-----------------------------------+
¦  Application Load Balancer (ALB)  ¦
¦  HTTPS + SSL (ACM certificate)    ¦
+-----------------------------------+
               ¦
               ?
+-----------------------------------+
¦  Amazon EC2  (Spring Boot)        ¦ ?-- IAM Role: EC2-Backend-Role
¦  - REST API (TransactionController)
¦  - FraudRuleEngine.java           ¦
¦  - JWT Auth (SecurityConfig.java) ¦
+-----------------------------------+
     ¦                 ¦
     ?                 ?
+---------+     +----------------------+
¦ Amazon  ¦     ¦ Amazon SQS           ¦
¦ RDS     ¦     ¦ Queue: tx-fraud-queue¦
¦Postgres ¦     +----------------------+
¦(SQL DB) ¦                ¦  triggers
+---------+                ?
                +----------------------+
                ¦ AWS Lambda           ¦ ?-- IAM Role: Lambda-Fraud-Role
                ¦ FraudVerificationFn  ¦
                ¦ (Python 3.11)        ¦
                +----------------------+
                     ¦          ¦
                     ?          ?
          +--------------+  +------------------+
          ¦  Amazon      ¦  ¦  Amazon RDS       ¦
          ¦  SageMaker   ¦  ¦  UPDATE           ¦
          ¦  Endpoint    ¦  ¦  fraud_status     ¦
          ¦  (ML Score)  ¦  ¦  fraud_score      ¦
          +--------------+  +------------------+
                 ¦ fraud probability (0.0-1.0)
                 ?
          +--------------+
          ¦ Amazon SNS   ¦--? Email / SMS to Customer
          ¦ Fraud Alert  ¦
          +--------------+

+------------------------------------------------------+
¦  Amazon S3  (Multi-purpose object storage)           ¦
¦  +-- finguard-frontend/   ? React build (dist/)      ¦
¦  +-- finguard-reports/    ? Transaction CSV reports  ¦
¦  +-- finguard-evidence/   ? Fraud case evidence logs ¦
¦  +-- finguard-ml-data/    ? SageMaker training data  ¦
+------------------------------------------------------+

+------------------------------------------------------+
¦  Amazon CloudWatch  (Monitoring & Observability)     ¦
¦  +-- EC2: CPU, memory, /finguard/backend logs        ¦
¦  +-- Lambda: invocations, errors, duration           ¦
¦  +-- RDS: connections, query latency, free storage   ¦
¦  +-- SQS: queue depth (ApproximateNumberOfMessages)  ¦
¦  +-- SageMaker: endpoint latency, error rate         ¦
¦  +-- Alarm ? SNS alert to dev team on any failure   ¦
+------------------------------------------------------+

+------------------------------------------------------+
¦  AWS IAM  (Identity & Access Management)             ¦
¦  +-- EC2-Backend-Role  ? SQS, RDS, S3, CloudWatch    ¦
¦  +-- Lambda-Fraud-Role ? SageMaker, RDS, SNS, SQS    ¦
¦  +-- SageMaker-Exec    ? S3 (read/write ML artifacts)¦
+------------------------------------------------------+
```

---

## AWS Services — Full Details

### 1. Amazon EC2 — Backend Application Server

**Purpose**: Hosts the Spring Boot backend. The primary compute layer of FinGuard.

| What runs on EC2 | Role in FinGuard |
|---|---|
| Spring Boot REST API | Accepts POST /api/transactions from frontend |
| `FraudRuleEngine.java` | Evaluates velocity, high-amount, location, device rules |
| `TransactionProducer.java` | Sends transaction JSON to SQS `tx-fraud-queue` |
| `TransactionRepository.java` | Reads/writes transaction and fraud data to RDS |
| `SecurityConfig.java` | JWT authentication on every API endpoint |

- Instance: `t3.medium` (dev) / `t3.large` (prod)
- Auto Scaling Group behind ALB (scales on CPU > 70%)
- IAM Instance Profile: `EC2-Backend-Role`

---

### 2. Amazon RDS (PostgreSQL 15) — Primary Database

**Purpose**: Stores all persistent transaction, user, and fraud alert data.

| Table | Data stored |
|---|---|
| `transactions` | Amount, status, `fraud_status`, `fraud_score`, `deviceId`, `location`, timestamp |
| `fraud_alerts` | `alertType`, `severity`, `reason`, `fraudScore`, `status` |
| `users` | Customer accounts, roles, email, phone |

- Multi-AZ in production · 7-day automated backups · Encryption at rest
- `database/schema.sql` ? table definitions
- `database/seed.sql` ? demo data seeded at first deployment

---

### 3. AWS Lambda — Serverless Fraud Processor

**Purpose**: Executes async fraud verification when triggered by SQS. No server to manage.

**Function name**: `FraudVerificationFn`
**Trigger**: SQS queue `tx-fraud-queue` (batch size: 1)
**Runtime**: Python 3.11 · Memory: 512 MB · Timeout: 30 seconds

**Execution flow**:
```
1. Receive transaction event from SQS
2. Run rule checks:
   - HIGH_AMOUNT    ? amount > ?50,000
   - VELOCITY       ? > 5 transactions in 60 seconds (countByUserIdAndTimestampAfter)
   - LOCATION       ? distance > 500km from last transaction
   - DEVICE_CHANGE  ? new deviceId not in findDistinctDeviceIdsByUserId
3. POST to SageMaker endpoint ? get fraud_probability (0.0–1.0)
4. final_score = (rule_score * 0.4) + (ml_score * 0.6)
5. UPDATE transactions SET fraud_status, fraud_score WHERE transaction_id = ?
6. If fraud_score > 0.7 ? INSERT into fraud_alerts
7. SNS.publish() ? notify customer
```

---

### 4. Amazon S3 — Object Storage

**Purpose**: Multi-purpose file storage across all FinGuard components.

| Bucket | Purpose |
|---|---|
| `finguard-frontend` | Hosts React `dist/` build served via CloudFront |
| `finguard-reports` | Monthly transaction CSV/PDF reports for users |
| `finguard-evidence` | Fraud case logs, screenshots, audit trail |
| `finguard-ml-data` | Training datasets uploaded for SageMaker model training |

- `finguard-frontend` ? public-read · all others ? private
- `finguard-ml-data` ? versioning enabled
- Lifecycle rule: reports > 90 days archived to S3 Glacier

---

### 5. Amazon CloudWatch — Monitoring & Observability

**Purpose**: Centralized logging, metrics, and alerting for every AWS service.

| Service | What is monitored |
|---|---|
| EC2 (Spring Boot) | CPU %, memory, disk I/O, `/finguard/backend` log group |
| Lambda | Invocations, error rate, duration, throttles |
| RDS | DB connections, read/write latency, free storage |
| SQS | Queue depth (`ApproximateNumberOfMessages`) |
| SageMaker | Endpoint latency, invocation errors |
| ALB | 5XX errors, request count, target response time |

**Alarms configured**:
- SQS depth > 1,000 ? scale EC2 Auto Scaling Group
- Lambda error rate > 5% in 5 min ? SNS alert to dev team
- RDS free storage < 1 GB ? SNS alert
- SageMaker endpoint latency > 2s ? SNS alert

---

### 6. Amazon SNS — Fraud Alert Notifications

**Purpose**: Sends real-time fraud alerts to customers and system alerts to the dev team.

**SNS Topics**:
| Topic | Subscribers |
|---|---|
| `finguard-fraud-alerts` | Customer email + SMS (registered at sign-up) |
| `finguard-system-alerts` | Dev team email (triggered by CloudWatch alarms) |

**Example customer alert**:
> ?? *"Alert: A suspicious transaction of ?85,000 was detected on your FinGuard account.*
> *Transaction ID: TXN-20240801-9821 · Severity: HIGH · Time: 14:32 IST.*
> *If this wasn't you, call 1800-FIN-GUARD immediately."*

---

### 7. AWS IAM — Identity & Access Management

**Purpose**: Secures all service-to-service calls with least-privilege permissions.

| IAM Role | Attached to | Permissions |
|---|---|---|
| `EC2-Backend-Role` | EC2 instance | `sqs:SendMessage`, `rds-db:connect`, `s3:PutObject`, `cloudwatch:PutMetricData` |
| `Lambda-Fraud-Role` | Lambda function | `sqs:ReceiveMessage`, `sagemaker:InvokeEndpoint`, `rds-db:connect`, `sns:Publish`, `secretsmanager:GetSecretValue` |
| `SageMaker-Exec-Role` | SageMaker | `s3:GetObject` (training data), `s3:PutObject` (model artifacts) |
| `CI-CD-Deploy-Role` | GitHub Actions | `ec2:*`, `lambda:UpdateFunctionCode`, `rds:*`, `s3:PutObject` |

- No hardcoded credentials in any source file
- MFA enforced on all human IAM users
- Secrets stored in **AWS Secrets Manager** (not in `.env`)

---

### 8. Amazon SQS — Message Queue (Async Decoupling)

**Purpose**: Buffers incoming transactions so Lambda processes them asynchronously, preventing overload.

**Queue**: `tx-fraud-queue` (Standard Queue)
**Dead-Letter Queue (DLQ)**: `tx-fraud-dlq` (captures messages that fail after 3 retries)

```
EC2 Spring Boot
  +- TransactionProducer.java
       +-? SQS: tx-fraud-queue
                 +-? Lambda: FraudVerificationFn (trigger)
                           ¦
                     [on failure]
                           +-? SQS DLQ: tx-fraud-dlq
```

- Visibility timeout: 30 sec (matches Lambda timeout)
- Max receive count: 3 retries before DLQ
- CloudWatch alarm: DLQ depth > 0 ? alert dev team

---

### 9. Amazon SageMaker — AI Fraud Detection Model

**Purpose**: Hosts the trained ML model as a scalable real-time inference endpoint.

**Model type**: Random Forest / XGBoost trained on labeled transaction data

**Training features** (from `transaction_schema.py`):
```
amount, hour_of_day, day_of_week, merchant_category,
is_new_device, location_distance_km, transactions_last_hour,
is_international, account_age_days
```

**SageMaker workflow**:
```
CSV training data ? S3 (finguard-ml-data/)
  +-? SageMaker Training Job (train_model.py)
          +-? Model artifact (.pkl) ? S3
                  +-? SageMaker Endpoint: finguard-fraud-endpoint
                            +-? Lambda POST /invocations
                                    +-? { "fraud_probability": 0.87 }
```

- Endpoint: `finguard-fraud-endpoint` (ml.t2.medium)
- Auto-scaling: min 1 / max 4 instances based on invocation count
- Lambda calls via `sagemaker-runtime` boto3 client

---

## VPC Network Design

```
VPC: 10.0.0.0/16
+-- Public Subnets  (ALB, NAT Gateway)
¦   +-- 10.0.1.0/24  (AZ-a)
¦   +-- 10.0.2.0/24  (AZ-b)
+-- Private Subnets (EC2, Lambda, RDS)
    +-- 10.0.10.0/24 (AZ-a)
    +-- 10.0.20.0/24 (AZ-b)
```

- EC2 and Lambda run in **private subnets** (not internet-accessible)
- RDS accessible only from EC2 and Lambda security groups
- SQS, SNS, SageMaker, S3 accessed via **VPC Endpoints** (no internet traversal)
- ALB in **public subnets** — the only entry point for external traffic

---

## Interview Summary (1 Minute)

> *"Our FinGuard Real-Time Transactions and Fraud Detection System is fully deployed on AWS.*
> *The Spring Boot backend runs on **Amazon EC2** behind an Application Load Balancer.*
> *Each transaction is stored in **Amazon RDS (PostgreSQL)** and pushed to **Amazon SQS**,*
> *which triggers an **AWS Lambda** function for async fraud processing.*
> *Lambda runs our FraudRuleEngine checks and calls an **Amazon SageMaker** endpoint*
> *for an AI-powered fraud probability score. If the score is HIGH, Lambda fires a*
> *customer alert via **Amazon SNS** — by email or SMS.*
> *Transaction reports and ML training datasets are stored in **Amazon S3**.*
> ***Amazon CloudWatch** monitors EC2 health, Lambda errors, SQS queue depth, and*
> *SageMaker latency, with automatic alarms. **AWS IAM** enforces least-privilege roles*
> *across every service — no service has more access than it absolutely needs.*
> *This architecture gives us scalability, serverless processing, and AI-powered*
> *fraud detection — all on AWS."*

---

## Deployment Checklist

- [ ] Launch EC2 instance and deploy Spring Boot JAR (`mvn package -DskipTests`)
- [ ] Create RDS PostgreSQL instance and apply `database/schema.sql`
- [ ] Seed demo data via `database/seed.sql`
- [ ] Create S3 buckets: `finguard-frontend`, `finguard-reports`, `finguard-evidence`, `finguard-ml-data`
- [ ] Build React app: `npm run build` ? sync `dist/` to `finguard-frontend`
- [ ] Create CloudFront distribution ? S3 `finguard-frontend` bucket (HTTPS via ACM)
- [ ] Create SQS queue `tx-fraud-queue` + DLQ `tx-fraud-dlq` (max receive count: 3)
- [ ] Upload ML training CSV to `finguard-ml-data/` ? run SageMaker Training Job
- [ ] Deploy SageMaker endpoint `finguard-fraud-endpoint` (ml.t2.medium)
- [ ] Deploy Lambda function `FraudVerificationFn` ? set SQS `tx-fraud-queue` as trigger
- [ ] Create SNS topics: `finguard-fraud-alerts` (customer), `finguard-system-alerts` (dev team)
- [ ] Subscribe customer email/SMS to `finguard-fraud-alerts`
- [ ] Create IAM roles: `EC2-Backend-Role`, `Lambda-Fraud-Role`, `SageMaker-Exec-Role`
- [ ] Store secrets in AWS Secrets Manager: `finguard/db-password`, `finguard/jwt-secret`
- [ ] Set up CloudWatch log groups `/finguard/backend`, `/finguard/lambda`
- [ ] Configure CloudWatch alarms (SQS depth, Lambda errors, RDS storage, SageMaker latency)
- [ ] Configure ALB ? EC2 Auto Scaling Group (target tracking CPU 70%)
- [ ] Test end-to-end: POST /api/transactions ? SQS queued ? Lambda triggered ? SageMaker scored ? SNS alert fired
