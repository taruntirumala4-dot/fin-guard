from fastapi import APIRouter, HTTPException, status
from schemas.transaction_schema import TransactionPredictionInput, PredictionOutput
from services.fraud_prediction_service import prediction_service

router = APIRouter()

@router.post(
    "/predict",
    response_model=PredictionOutput,
    status_code=status.HTTP_200_OK,
    summary="Predict transaction fraud probability",
    description="Accepts transaction feature parameters and returns fraud probability using a trained Random Forest model."
)
async def predict_fraud(transaction: TransactionPredictionInput):
    try:
        result = prediction_service.predict(transaction)
        return result
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Prediction error: {str(e)}"
        )
