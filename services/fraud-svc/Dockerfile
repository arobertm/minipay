FROM python:3.12-slim

WORKDIR /app

# Install dependencies first (cached layer)
COPY services/fraud-svc/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY services/fraud-svc/app/ ./app/

# Model store directory (persisted via Docker volume)
RUN mkdir -p /app/model_store

EXPOSE 8090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
