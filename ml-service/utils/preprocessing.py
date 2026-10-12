import pandas as pd
import numpy as np

TYPE_MAPPING = {
    "PAYMENT": 0,
    "TRANSFER": 1,
    "WITHDRAWAL": 2,
    "DEPOSIT": 3
}

FEATURE_COLUMNS = [
    "amount",
    "transaction_frequency",
    "location_change",
    "device_change",
    "transaction_type_code",
    "hour_of_day",
    "account_age_days",
    "historical_fraud_count"
]

def preprocess_input_dict(data: dict) -> pd.DataFrame:
    """
    Converts input dictionary into a pandas DataFrame ready for Scikit-learn prediction.
    """
    tx_type = data.get("transaction_type", "PAYMENT").upper()
    tx_code = TYPE_MAPPING.get(tx_type, 0)
    
    row = {
        "amount": float(data.get("amount", 0.0)),
        "transaction_frequency": int(data.get("transaction_frequency", 1)),
        "location_change": int(data.get("location_change", 0)),
        "device_change": int(data.get("device_change", 0)),
        "transaction_type_code": tx_code,
        "hour_of_day": int(data.get("hour_of_day", 12)),
        "account_age_days": int(data.get("account_age_days", 180)),
        "historical_fraud_count": int(data.get("historical_fraud_count", 0))
    }
    
    df = pd.DataFrame([row], columns=FEATURE_COLUMNS)
    return df
