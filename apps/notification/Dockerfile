FROM python:3.11-slim

WORKDIR /app
COPY main.py .

RUN pip install --no-cache-dir fastapi uvicorn gunicorn python-dotenv pydantic[email]

CMD ["gunicorn", "-w", "4", "-k", "uvicorn.workers.UvicornWorker", "main:app"]