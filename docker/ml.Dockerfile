# ============================================================
#  ML Service Dockerfile – Python FastAPI
# ============================================================

FROM python:3.11-slim

WORKDIR /app

# System dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Security: non-root user
RUN useradd -m -u 1000 mluser
USER mluser
ENV PATH="/home/mluser/.local/bin:${PATH}"

# Install Python dependencies
COPY --chown=mluser:mluser requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# Copy application code
COPY --chown=mluser:mluser . .

EXPOSE 8000

# If model doesn't exist, train it first; then start API
CMD ["sh", "-c", "python model/train_model.py && uvicorn app:app --host 0.0.0.0 --port 8000"]
