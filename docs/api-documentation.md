# REST API Documentation – FinGuard

Base URL: `http://localhost:8080`

---

## Authentication Endpoints

### 1. Register User
- **POST** `/api/auth/register`
- **Auth**: None
- **Body**:
  ```json
  {
    "name": "Priya Sharma",
    "email": "priya.sharma@example.com",
    "password": "Password@123",
    "confirmPassword": "Password@123"
  }
  ```
- **Response** `(201 Created)`:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 5,
    "name": "Priya Sharma",
    "email": "priya.sharma@example.com",
    "role": "USER",
    "expiresIn": 86400000
  }
  ```

### 2. Login User
- **POST** `/api/auth/login`
- **Auth**: None
- **Body**:
  ```json
  {
    "email": "admin@finguard.io",
    "password": "Password@123"
  }
  ```
- **Response** `(200 OK)`:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "userId": 1,
    "name": "Admin User",
    "email": "admin@finguard.io",
    "role": "ADMIN",
    "expiresIn": 86400000
  }
  ```

---

## Transaction Endpoints

### 3. Create Transaction
- **POST** `/api/transactions`
- **Auth**: Bearer Token
- **Body**:
  ```json
  {
    "amount": 25000,
    "currency": "INR",
    "merchant": "Amazon India",
    "location": "Hyderabad",
    "transactionType": "PAYMENT",
    "deviceId": "DEV-HP-LAPTOP-001"
  }
  ```
- **Response** `(201 Created)`:
  ```json
  {
    "transactionId": "TXN-20260731-A1B2C3D4",
    "userId": 5,
    "userName": "Priya Sharma",
    "amount": 25000.00,
    "currency": "INR",
    "merchant": "Amazon India",
    "location": "Hyderabad",
    "transactionType": "PAYMENT",
    "deviceId": "DEV-HP-LAPTOP-001",
    "status": "PENDING",
    "fraudScore": 0.00,
    "fraudStatus": "SAFE"
  }
  ```

### 4. Search Transactions
- **GET** `/api/transactions/search?merchant=Amazon&fraudStatus=SAFE&page=0&size=20`
- **Auth**: Bearer Token

---

## Fraud & Analytics Endpoints

### 5. Get Fraud Alerts
- **GET** `/api/fraud/alerts?page=0&size=20`
- **Auth**: Bearer Token (ANALYST, ADMIN)

### 6. Analyze Transaction
- **POST** `/api/fraud/analyze/{transactionId}`
- **Auth**: Bearer Token (ANALYST, ADMIN)
- **Response** `(200 OK)`:
  ```json
  {
    "ruleScore": 30.0,
    "mlScore": 87.4,
    "finalScore": 64.44,
    "fraudStatus": "FRAUD",
    "reasons": ["Transaction amount ₹250,000 exceeds threshold ₹100,000"],
    "alertType": "HIGH_AMOUNT",
    "mlServiceAvailable": true
  }
  ```

### 7. Dashboard Summary
- **GET** `/api/dashboard/summary`
- **Auth**: Bearer Token
