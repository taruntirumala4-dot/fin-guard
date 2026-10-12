import os
import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, roc_auc_score
import joblib

def generate_synthetic_data(num_samples=5000, seed=42):
    np.random.seed(seed)
    
    amount = np.random.exponential(scale=15000, size=num_samples) + 100
    # Inject some high amounts
    high_idx = np.random.choice(num_samples, size=int(num_samples * 0.05), replace=False)
    amount[high_idx] += np.random.uniform(100000, 400000, size=len(high_idx))
    
    transaction_frequency = np.random.poisson(lam=2, size=num_samples)
    freq_idx = np.random.choice(num_samples, size=int(num_samples * 0.05), replace=False)
    transaction_frequency[freq_idx] += np.random.randint(5, 15, size=len(freq_idx))
    
    location_change = np.random.binomial(n=1, p=0.08, size=num_samples)
    device_change = np.random.binomial(n=1, p=0.10, size=num_samples)
    transaction_type_code = np.random.choice([0, 1, 2, 3], size=num_samples, p=[0.6, 0.25, 0.1, 0.05])
    hour_of_day = np.random.randint(0, 24, size=num_samples)
    account_age_days = np.random.randint(1, 1000, size=num_samples)
    historical_fraud_count = np.random.choice([0, 1, 2], size=num_samples, p=[0.92, 0.06, 0.02])
    
    # Calculate synthetic target (is_fraud) based on realistic rule-based logic + noise
    fraud_prob = (
        (amount > 100000) * 0.45 +
        (transaction_frequency > 5) * 0.35 +
        (location_change == 1) * 0.30 +
        (device_change == 1) * 0.20 +
        (historical_fraud_count > 0) * 0.35 +
        ((hour_of_day >= 1) & (hour_of_day <= 4)) * 0.15
    )
    
    # Add random noise
    fraud_prob = np.clip(fraud_prob + np.random.normal(0, 0.05, size=num_samples), 0, 1)
    is_fraud = (fraud_prob > 0.45).astype(int)
    
    df = pd.DataFrame({
        "amount": amount,
        "transaction_frequency": transaction_frequency,
        "location_change": location_change,
        "device_change": device_change,
        "transaction_type_code": transaction_type_code,
        "hour_of_day": hour_of_day,
        "account_age_days": account_age_days,
        "historical_fraud_count": historical_fraud_count,
        "is_fraud": is_fraud
    })
    return df

def train():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(current_dir, "..", "data")
    os.makedirs(data_dir, exist_ok=True)
    
    csv_path = os.path.join(data_dir, "transactions.csv")
    if not os.path.exists(csv_path):
        print("Generating synthetic transactions dataset...")
        df = generate_synthetic_data()
        df.to_csv(csv_path, index=False)
    else:
        print(f"Loading dataset from {csv_path}...")
        df = pd.read_csv(csv_path)

    X = df.drop(columns=["is_fraud"])
    y = df["is_fraud"]

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42, stratify=y)

    model = RandomForestClassifier(
        n_estimators=100,
        max_depth=10,
        random_state=42,
        class_weight="balanced"
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]

    print("\n--- Model Evaluation ---")
    print(classification_report(y_test, y_pred))
    print(f"ROC-AUC Score: {roc_auc_score(y_test, y_prob):.4f}")

    model_path = os.path.join(current_dir, "fraud_model.pkl")
    joblib.dump(model, model_path)
    print(f"\nModel successfully saved to: {model_path}")

if __name__ == "__main__":
    train()
