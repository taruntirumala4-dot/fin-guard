import pytest
from fastapi.testclient import TestClient
from app import app

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "ok"
    assert "FinGuard ML Service" in data["service"]

def test_prediction_normal_transaction():
    payload = {
        "amount": 1500.0,
        "transaction_frequency": 1,
        "location_change": 0,
        "device_change": 0,
        "transaction_type": "PAYMENT"
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "fraud_probability" in data
    assert "prediction" in data
    assert 0.0 <= data["fraud_probability"] <= 1.0

def test_prediction_high_risk_transaction():
    payload = {
        "amount": 350000.0,
        "transaction_frequency": 8,
        "location_change": 1,
        "device_change": 1,
        "transaction_type": "TRANSFER",
        "historical_fraud_count": 2
    }
    response = client.post("/predict", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["fraud_probability"] > 0.4
    assert data["prediction"] in ["FRAUD", "SAFE"]
