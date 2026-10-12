from model.predict import FraudPredictor
from utils.preprocessing import preprocess_input_dict
from schemas.transaction_schema import TransactionPredictionInput, PredictionOutput

class FraudPredictionService:
    def __init__(self):
        self.predictor = None

    def load_model(self):
        if self.predictor is None:
            self.predictor = FraudPredictor()

    def predict(self, input_data: TransactionPredictionInput) -> PredictionOutput:
        self.load_model()
        data_dict = input_data.model_dump()
        df = preprocess_input_dict(data_dict)
        
        prob = self.predictor.predict_proba(df)
        prediction = "FRAUD" if prob >= 0.5 else "SAFE"
        confidence = prob if prediction == "FRAUD" else (1.0 - prob)
        
        return PredictionOutput(
            fraud_probability=round(prob, 4),
            prediction=prediction,
            confidence=round(confidence, 4)
        )

prediction_service = FraudPredictionService()
