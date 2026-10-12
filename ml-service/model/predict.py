import os
import joblib
import pandas as pd

class FraudPredictor:
    def __init__(self, model_path: str = None):
        if model_path is None:
            current_dir = os.path.dirname(os.path.abspath(__file__))
            model_path = os.path.join(current_dir, "fraud_model.pkl")
            
        if not os.path.exists(model_path):
            raise FileNotFoundError(f"Model file not found at {model_path}. Please run train_model.py first.")
            
        self.model = joblib.load(model_path)

    def predict_proba(self, df: pd.DataFrame) -> float:
        """
        Returns probability of fraud (class 1).
        """
        probabilities = self.model.predict_proba(df)
        fraud_prob = float(probabilities[0][1])
        return fraud_prob
