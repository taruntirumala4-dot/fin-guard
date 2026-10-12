# AWS Services – FinGuard
## Real-Time Transactions & Fraud Detection System

---

## Service Mapping: Local ? AWS

| Component | Local (Docker Compose) | AWS Service |
|---|---|---|
| Spring Boot Backend | `finguard-backend` container | **Amazon EC2** (Auto Scaling Group + ALB) |
| ML Fraud Scoring | Python FastAPI (`fraud_model.pkl`) | **Amazon SageMaker** (managed endpoint) |
| Primary Database | `postgres:15-alpine` | **Amazon RDS** (PostgreSQL 15, Multi-AZ) |
| Transaction Queue | (none / sync call) | **Amazon SQS** (`tx-fraud-queue`) |
| Fraud Processor | (inline Spring Boot logic) | **AWS Lambda** (`FraudVerificationFn`) |
| React Frontend | `nginx:alpine` | **Amazon S3** + CloudFront |
| File Storage | Local disk / volume | **Amazon S3** (reports, evidence, ML data) |
| Fraud Alerts | Console log | **Amazon SNS** (Email/SMS to customer) |
| Logging & Monitoring | Docker logs | **Amazon CloudWatch** |
| Secrets / Credentials | `.env` file | AWS Secrets Manager |
| Access Control | (none) | **AWS IAM** (roles + policies) |

---

## Services — Purpose & FinGuard Usage

---

### 1. Amazon EC2 — Backend Compute

**Why EC2?** EC2 gives full control over the server environment where Spring Boot runs.

**What runs on it:**
- `FinGuardApplication.java` — Spring Boot entry point
- `TransactionController.java` — handles `POST /api/transactions`
- `FraudRuleEngine.java` — orchestrates all `FraudRule` strategy implementations
- `TransactionProducer.java` — publishes transaction JSON to SQS `tx-fraud-queue`
- `SecurityConfig.java` — JWT authentication on every endpoint

**Configuration:**
- Instance: `t3.medium` (dev) / `t3.large` (prod)
- Deployed behind **Application Load Balancer** (HTTPS termination via ACM)
- Auto Scaling Group: min 1 / max 4 instances · scale on CPU > 70%
- IAM Instance Profile: `EC2-Backend-Role`

---

### 2. Amazon RDS (PostgreSQL 15) — Database

**Why RDS?** Managed, highly available PostgreSQL with automatic backups and Multi-AZ failover.

**Tables used by FinGuard:**

| Table | Key columns |
|---|---|
| `transactions` | `transaction_id`, `amount`, `fraud_status`, `fraud_score`, `device_id`, `location` |
| `fraud_alerts` | `alert_type`, `severity`, `reason`, `fraud_score`, `status` |
| `users` | `user_id`, `email`, `phone`, `role` |

**Java integration:**
- `TransactionRepository.java` ? `findByUserIdAndTimestampAfter()` (velocity check)
- `TransactionRepository.java` ? `findTop1ByUserIdAndTimestampBefore()` (location mismatch)
- `TransactionRepository.java` ? `findDistinctDeviceIdsByUserId()` (device change)

**Config:** Multi-AZ · db.t3.micro (dev) · 7-day backups · encryption at rest

---

### 3. AWS Lambda — Serverless Fraud Processor

**Why Lambda?** Executes fraud logic on-demand per transaction — no idle server cost, infinite concurrency.

**Function:** `FraudVerificationFn` · Runtime: Python 3.11 · Memory: 512 MB · Timeout: 30s

**Trigger:** SQS `tx-fraud-queue` (event source mapping, batch size: 1)

**What Lambda does:**
```
1. Parse transaction JSON from SQS message
2. Query RDS ? run fraud rule checks (velocity, amount, location, device)
3. Call SageMaker endpoint ? get fraud_probability (0.0 – 1.0)
4. Compute final score: (rule_score × 0.4) + (ml_probability × 0.6) × 100
5. UPDATE transactions SET fraud_status, fraud_score in RDS
6. If fraud_score > 70 ? INSERT fraud_alert in RDS
7. SNS.publish(topic=finguard-fraud-alerts, message=alert_text)
```

**IAM Role:** `Lambda-Fraud-Role` ? SageMaker invoke, RDS connect, SNS publish, SQS delete

---

### 4. Amazon S3 — Object Storage

**Why S3?** Cheap, durable, unlimited storage for all file-based assets across FinGuard.

| Bucket | What's stored |
|---|---|
| `finguard-frontend` | React `dist/` build (served via CloudFront CDN) |
| `finguard-reports` | Transaction history PDF/CSV reports |
| `finguard-evidence` | Fraud case audit logs and screenshots |
| `finguard-ml-data` | Training dataset CSVs uploaded for SageMaker |

- `finguard-frontend` ? public-read (CloudFront origin)
- All others ? private (IAM-controlled access only)
- `finguard-ml-data` ? versioning enabled
- Lifecycle: reports > 90 days ? S3 Glacier

---

### 5. Amazon CloudWatch — Monitoring & Observability

**Why CloudWatch?** Single pane of glass for all AWS service logs, metrics, and alarms.

| Service | Log / Metric |
|---|---|
| EC2 Spring Boot | `/finguard/backend` log group · CPU %, memory |
| Lambda | Invocations, errors, duration, concurrency |
| RDS | DB connections, query latency, storage |
| SQS | `ApproximateNumberOfMessages` (queue depth) |
| SageMaker | Endpoint invocation latency, error rate |
| ALB | 5XX errors, target response time |

**Alarms:**
- Lambda error rate > 5% ? notify `finguard-system-alerts` SNS
- SQS depth > 1,000 ? trigger EC2 scale-out
- RDS free storage < 1 GB ? notify dev team
- SageMaker latency > 2s ? notify dev team

---

### 6. Amazon SNS — Notifications

**Why SNS?** Pub/Sub service that fans out a single fraud alert to multiple subscriber types.

| SNS Topic | Subscribers |
|---|---|
| `finguard-fraud-alerts` | Customer email + SMS (subscribed at registration) |
| `finguard-system-alerts` | Dev team email (subscribed to CloudWatch alarms) |

**Example alert published by Lambda:**
```json
{
  "transactionId": "TXN-20240801-9821",
  "amount": 85000,
  "severity": "HIGH",
  "fraudScore": 87.4,
  "reason": "High amount + New device + Velocity breach",
  "timestamp": "2024-08-01T14:32:00Z"
}
```
> Customer receives: *"?? Suspicious transaction of ?85,000 detected. Severity: HIGH."*

---

### 7. AWS IAM — Identity & Access Management

**Why IAM?** Every AWS service communication must be authenticated and authorized.

| IAM Role | Attached to | Key Permissions |
|---|---|---|
| `EC2-Backend-Role` | EC2 instance | `sqs:SendMessage`, `s3:PutObject`, `cloudwatch:PutMetricData` |
| `Lambda-Fraud-Role` | Lambda function | `sagemaker:InvokeEndpoint`, `sns:Publish`, `rds-db:connect`, `sqs:DeleteMessage` |
| `SageMaker-Exec-Role` | SageMaker | `s3:GetObject` (training data), `s3:PutObject` (model artifacts) |
| `CI-CD-Deploy-Role` | GitHub Actions / CI | `lambda:UpdateFunctionCode`, `ec2:DescribeInstances`, `s3:PutObject` |

- No hardcoded credentials in any source file
- All secrets stored in **AWS Secrets Manager**, retrieved at runtime

---

### 8. Amazon SQS — Message Queue

**Why SQS?** Decouples the EC2 transaction API from the Lambda fraud processor.
Prevents system overload during peak transaction volumes.

**Queue:** `tx-fraud-queue` (Standard Queue)
**DLQ:** `tx-fraud-dlq` (Dead-Letter Queue — catches failed messages after 3 retries)

**Message flow:**
```
TransactionProducer.java (EC2)
  +-? SQS: tx-fraud-queue
          +-? Lambda trigger: FraudVerificationFn
                    ¦ (on repeated failure)
                    +-? SQS DLQ: tx-fraud-dlq
                              +-? CloudWatch alarm ? dev team alert
```

**Settings:**
- Visibility timeout: 30 seconds (matches Lambda timeout)
- Max receive count: 3 (then moves to DLQ)
- CloudWatch: DLQ depth > 0 ? immediate dev alert

---

### 9. Amazon SageMaker — Machine Learning

**Why SageMaker?** Hosts the fraud detection ML model as a managed, scalable, auto-scaling endpoint.
Eliminates the need to self-host the Python FastAPI ML microservice in production.

**Model training** (from `ml-service/model/train_model.py`):
- Algorithm: Random Forest / XGBoost
- Training data: `finguard-ml-data/` in S3
- Output: `fraud_model.pkl` artifact stored back to S3

**Features used** (from `transaction_schema.py`):
```
amount, hour_of_day, day_of_week, merchant_category,
is_new_device, location_distance_km, transactions_last_hour,
is_international, account_age_days
```

**Endpoint invocation by Lambda:**
```python
import boto3, json

client = boto3.client("sagemaker-runtime")
response = client.invoke_endpoint(
    EndpointName="finguard-fraud-endpoint",
    ContentType="application/json",
    Body=json.dumps(transaction_features)
)
result = json.loads(response["Body"].read())
fraud_probability = result["fraud_probability"]  # e.g. 0.87
```

**Endpoint config:**
- Name: `finguard-fraud-endpoint`
- Instance: `ml.t2.medium`
- Auto-scaling: min 1 / max 4 (based on invocations per minute)
