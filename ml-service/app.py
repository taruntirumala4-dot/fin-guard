import os
from fastapi import FastAPI
from routes.prediction import router as prediction_router
from routes.health import router as health_router
from services.fraud_prediction_service import prediction_service

app = FastAPI(
    title="FinGuard ML Service",
    description="Machine Learning Microservice for Real-Time Fraud Detection Probability",
    version="1.0.0"
)

@app.on_event("startup")
def startup_event():
    current_dir = os.path.dirname(os.path.abspath(__file__))
    model_path = os.path.join(current_dir, "model", "fraud_model.pkl")
    
    if not os.path.exists(model_path):
        print("Model file missing. Running train_model.py...")
        from model.train_model import train
        train()
        
    prediction_service.load_model()
    print("FastAPI ML service started and model loaded into memory.")

app.include_router(health_router, tags=["Health"])
app.include_router(prediction_router, tags=["Prediction"])

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)
