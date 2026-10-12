from fastapi import APIRouter
from datetime import datetime

router = APIRouter()

@router.get(
    "/health",
    summary="Service Health Check",
    description="Returns status of the FastAPI ML Fraud Detection service."
)
async def health_check():
    return {
        "status": "ok",
        "service": "FinGuard ML Service",
        "timestamp": datetime.utcnow().isoformat()
    }
