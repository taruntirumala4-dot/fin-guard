from pydantic import BaseModel, Field, field_validator
from typing import Optional

class TransactionPredictionInput(BaseModel):
    amount: float = Field(..., gt=0, description="Transaction amount")
    transaction_frequency: int = Field(..., ge=0, description="Transactions count in last window")
    location_change: int = Field(0, ge=0, le=1, description="1 if location mismatch detected, else 0")
    device_change: int = Field(0, ge=0, le=1, description="1 if new device detected, else 0")
    transaction_type: str = Field("PAYMENT", description="PAYMENT, TRANSFER, WITHDRAWAL, DEPOSIT")
    hour_of_day: Optional[int] = Field(12, ge=0, le=23, description="Hour of the day 0-23")
    account_age_days: Optional[int] = Field(180, ge=0, description="Age of account in days")
    historical_fraud_count: Optional[int] = Field(0, ge=0, description="Past fraud flags for user")

    @field_validator('transaction_type')
    def validate_type(cls, v):
        allowed = {'PAYMENT', 'TRANSFER', 'WITHDRAWAL', 'DEPOSIT'}
        upper_v = v.upper()
        if upper_v not in allowed:
            raise ValueError(f"transaction_type must be one of {allowed}")
        return upper_v

class PredictionOutput(BaseModel):
    fraud_probability: float = Field(..., ge=0.0, le=1.0, description="Calculated probability of fraud")
    prediction: str = Field(..., description="FRAUD or SAFE")
    confidence: float = Field(..., ge=0.0, le=1.0, description="Model confidence score")
