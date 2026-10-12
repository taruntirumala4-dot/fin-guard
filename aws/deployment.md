# AWS Deployment Guide – FinGuard
## Real-Time Transactions & Fraud Detection System

---

## Prerequisites

- AWS CLI installed and configured (`aws configure` with ap-south-1 region)
- Java 21 + Maven installed (for Spring Boot build)
- Node.js 18+ installed (for React build)
- Python 3.11+ installed (for Lambda function packaging)
- AWS account with admin permissions (or specific IAM deploy role)

---

## Step 1: Create IAM Roles

```bash
# EC2 Backend Role — allows SQS send, S3 write, CloudWatch Logs
aws iam create-role --role-name EC2-Backend-Role \
  --assume-role-policy-document file://iam/ec2-trust-policy.json

aws iam attach-role-policy --role-name EC2-Backend-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSQSFullAccess
aws iam attach-role-policy --role-name EC2-Backend-Role \
  --policy-arn arn:aws:iam::aws:policy/CloudWatchLogsFullAccess
aws iam attach-role-policy --role-name EC2-Backend-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

# Lambda Fraud Role — allows SageMaker invoke, RDS, SNS, SQS, Secrets Manager
aws iam create-role --role-name Lambda-Fraud-Role \
  --assume-role-policy-document file://iam/lambda-trust-policy.json

aws iam attach-role-policy --role-name Lambda-Fraud-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSageMakerFullAccess
aws iam attach-role-policy --role-name Lambda-Fraud-Role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSNSFullAccess
aws iam attach-role-policy --role-name Lambda-Fraud-Role \
  --policy-arn arn:aws:iam::aws:policy/SecretsManagerReadWrite
```

---

## Step 2: Create RDS PostgreSQL Instance

```bash
aws rds create-db-instance \
  --db-instance-identifier finguard-postgres \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15 \
  --master-username postgres \
  --master-user-password <STRONG_PASSWORD> \
  --db-name finguard \
  --allocated-storage 20 \
  --vpc-security-group-ids <SECURITY_GROUP_ID> \
  --db-subnet-group-name <SUBNET_GROUP> \
  --no-publicly-accessible \
  --backup-retention-period 7 \
  --storage-encrypted

# Wait for RDS to be available, then apply schema and seed data
psql -h <RDS_ENDPOINT> -U postgres -d finguard -f database/schema.sql
psql -h <RDS_ENDPOINT> -U postgres -d finguard -f database/seed.sql
```

---

## Step 3: Create S3 Buckets

```bash
# Frontend bucket (public, CloudFront origin)
aws s3 mb s3://finguard-frontend --region ap-south-1

# Private buckets for reports, evidence, and ML training data
aws s3 mb s3://finguard-reports --region ap-south-1
aws s3 mb s3://finguard-evidence --region ap-south-1
aws s3 mb s3://finguard-ml-data --region ap-south-1

# Enable versioning on ML data bucket
aws s3api put-bucket-versioning \
  --bucket finguard-ml-data \
  --versioning-configuration Status=Enabled

# Upload React frontend build
cd frontend
VITE_API_BASE_URL=https://api.your-domain.com npm run build
aws s3 sync dist/ s3://finguard-frontend --delete
```

---

## Step 4: Train and Deploy SageMaker Model

```bash
# 1. Upload training data to S3
aws s3 cp ml-service/data/transactions_labeled.csv \
  s3://finguard-ml-data/training/transactions_labeled.csv

# 2. Run SageMaker Training Job (using ml-service/model/train_model.py)
aws sagemaker create-training-job \
  --training-job-name finguard-fraud-training \
  --algorithm-specification TrainingImage=<SKLearn_IMAGE_URI>,TrainingInputMode=File \
  --role-arn arn:aws:iam::<ACCOUNT_ID>:role/SageMaker-Exec-Role \
  --input-data-config '[{"ChannelName":"train","DataSource":{"S3DataSource":{"S3Uri":"s3://finguard-ml-data/training/","S3DataType":"S3Prefix"}}}]' \
  --output-data-config '{"S3OutputPath":"s3://finguard-ml-data/model/"}' \
  --resource-config '{"InstanceType":"ml.m5.large","InstanceCount":1,"VolumeSizeInGB":10}'

# 3. Deploy trained model as real-time endpoint
aws sagemaker create-endpoint-config \
  --endpoint-config-name finguard-fraud-config \
  --production-variants '[{"VariantName":"primary","ModelName":"finguard-fraud-model","InstanceType":"ml.t2.medium","InitialInstanceCount":1}]'

aws sagemaker create-endpoint \
  --endpoint-name finguard-fraud-endpoint \
  --endpoint-config-name finguard-fraud-config
```

---

## Step 5: Create SQS Queue and DLQ

```bash
# Dead-Letter Queue (captures failed messages after 3 retries)
aws sqs create-queue \
  --queue-name tx-fraud-dlq \
  --region ap-south-1

DLQ_ARN=$(aws sqs get-queue-attributes \
  --queue-url https://sqs.ap-south-1.amazonaws.com/<ACCOUNT_ID>/tx-fraud-dlq \
  --attribute-names QueueArn \
  --query 'Attributes.QueueArn' --output text)

# Main fraud processing queue
aws sqs create-queue \
  --queue-name tx-fraud-queue \
  --attributes "{
    \"VisibilityTimeout\": \"30\",
    \"RedrivePolicy\": \"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"3\\\"}\"
  }"
```

---

## Step 6: Deploy Lambda Function

```bash
# Package Lambda function
cd lambda/fraud-verification
pip install boto3 psycopg2-binary -t ./package
cp lambda_function.py ./package/
cd package && zip -r ../FraudVerificationFn.zip . && cd ..

# Create Lambda function
aws lambda create-function \
  --function-name FraudVerificationFn \
  --runtime python3.11 \
  --handler lambda_function.lambda_handler \
  --role arn:aws:iam::<ACCOUNT_ID>:role/Lambda-Fraud-Role \
  --zip-file fileb://FraudVerificationFn.zip \
  --timeout 30 \
  --memory-size 512 \
  --environment 'Variables={
    RDS_ENDPOINT=<RDS_ENDPOINT>,
    RDS_DB=finguard,
    SNS_TOPIC_ARN=arn:aws:sns:ap-south-1:<ACCOUNT_ID>:finguard-fraud-alerts,
    SAGEMAKER_ENDPOINT=finguard-fraud-endpoint
  }'

# Set SQS as event source trigger for Lambda
aws lambda create-event-source-mapping \
  --function-name FraudVerificationFn \
  --event-source-arn arn:aws:sqs:ap-south-1:<ACCOUNT_ID>:tx-fraud-queue \
  --batch-size 1
```

---

## Step 7: Create SNS Topics

```bash
# Customer fraud alerts
aws sns create-topic --name finguard-fraud-alerts --region ap-south-1

# Dev team system alerts (CloudWatch alarms point here)
aws sns create-topic --name finguard-system-alerts --region ap-south-1

# Subscribe dev team email to system alerts
aws sns subscribe \
  --topic-arn arn:aws:sns:ap-south-1:<ACCOUNT_ID>:finguard-system-alerts \
  --protocol email \
  --notification-endpoint devteam@finguard.com
```

---

## Step 8: Launch EC2 and Deploy Spring Boot

```bash
# Build Spring Boot JAR
mvn clean package -DskipTests -f backend/pom.xml
# Output: backend/target/finguard-backend-1.0.0.jar

# Launch EC2 instance
aws ec2 run-instances \
  --image-id ami-0c2af51e265bd5e0b \
  --instance-type t3.medium \
  --key-name finguard-keypair \
  --security-group-ids <SG_ID> \
  --subnet-id <PRIVATE_SUBNET_ID> \
  --iam-instance-profile Name=EC2-Backend-Role \
  --user-data file://aws/ec2-userdata.sh

# Copy JAR to EC2 and start application
scp -i finguard-keypair.pem \
  backend/target/finguard-backend-1.0.0.jar \
  ec2-user@<EC2_IP>:/app/

ssh -i finguard-keypair.pem ec2-user@<EC2_IP>
java -jar /app/finguard-backend-1.0.0.jar \
  --spring.datasource.url=jdbc:postgresql://<RDS_ENDPOINT>:5432/finguard \
  --spring.datasource.password=$(aws secretsmanager get-secret-value \
      --secret-id finguard/db-password --query SecretString --output text) \
  --cloud.aws.sqs.queue-url=https://sqs.ap-south-1.amazonaws.com/<ACCOUNT_ID>/tx-fraud-queue
```

---

## Step 9: Store Secrets in AWS Secrets Manager

```bash
# Database password
aws secretsmanager create-secret \
  --name finguard/db-password \
  --secret-string '{"password":"<STRONG_PASSWORD>"}'

# JWT signing secret (min 256-bit)
aws secretsmanager create-secret \
  --name finguard/jwt-secret \
  --secret-string '{"secret":"<JWT_256BIT_SECRET>"}'
```

---

## Step 10: Configure CloudWatch

```bash
# Create log groups
aws logs create-log-group --log-group-name /finguard/backend
aws logs create-log-group --log-group-name /finguard/lambda

# Alarm: Lambda error rate > 5%
aws cloudwatch put-metric-alarm \
  --alarm-name finguard-lambda-errors \
  --metric-name Errors \
  --namespace AWS/Lambda \
  --dimensions Name=FunctionName,Value=FraudVerificationFn \
  --statistic Sum \
  --period 300 \
  --threshold 5 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --alarm-actions arn:aws:sns:ap-south-1:<ACCOUNT_ID>:finguard-system-alerts

# Alarm: SQS queue depth > 1000
aws cloudwatch put-metric-alarm \
  --alarm-name finguard-sqs-depth \
  --metric-name ApproximateNumberOfMessagesVisible \
  --namespace AWS/SQS \
  --dimensions Name=QueueName,Value=tx-fraud-queue \
  --statistic Maximum \
  --period 60 \
  --threshold 1000 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1 \
  --alarm-actions arn:aws:sns:ap-south-1:<ACCOUNT_ID>:finguard-system-alerts
```

---

## End-to-End Test

```bash
# Submit a test transaction
curl -X POST https://api.your-domain.com/api/transactions \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 85000,
    "merchantName": "Unknown Merchant",
    "category": "TRANSFER",
    "location": "Mumbai",
    "deviceId": "device-xyz-new"
  }'

# Expected flow:
# 1. EC2 Spring Boot receives ? saves to RDS ? sends to SQS
# 2. SQS triggers Lambda (FraudVerificationFn)
# 3. Lambda runs rule checks ? calls SageMaker ? gets fraud_probability: 0.87
# 4. Lambda updates RDS: fraud_status=FLAGGED, fraud_score=87.4
# 5. Lambda publishes to SNS: finguard-fraud-alerts
# 6. Customer receives Email/SMS alert
# 7. CloudWatch logs all steps in /finguard/lambda
```
