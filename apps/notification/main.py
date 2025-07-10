from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, EmailStr
from typing import List
import smtplib
from email.mime.text import MIMEText
from datetime import datetime
import os

app = FastAPI()

# Модель для входящего сообщения
class NotificationRequest(BaseModel):
    email: EmailStr
    subject: str
    message: str

# Модель для хранения уведомления
class Notification(BaseModel):
    id: int
    email: str
    subject: str
    message: str
    sent_at: datetime

class ServiceState(BaseModel):
    status: str
    message: str

# "База данных" в памяти
notifications_db = []
next_id = 1

# Конфигурация SMTP (для отправки email)
SMTP_SERVER = os.getenv("SMTP_SERVER", "smtp.example.com")
SMTP_PORT = int(os.getenv("SMTP_PORT", 587))
SMTP_USERNAME = os.getenv("SMTP_USERNAME", "your_email@example.com")
SMTP_PASSWORD = os.getenv("SMTP_PASSWORD", "your_password")

@app.post("/notifications", response_model=Notification)
async def send_notification(request: NotificationRequest):
    global next_id

    # Если не заглушка, то пробуем отправить настоящий email
    if SMTP_SERVER != "mock":
        try:
            msg = MIMEText(request.message)
            msg["Subject"] = request.subject
            msg["From"] = SMTP_USERNAME
            msg["To"] = request.email

            with smtplib.SMTP(SMTP_SERVER, SMTP_PORT) as server:
                server.starttls()
                server.login(SMTP_USERNAME, SMTP_PASSWORD)
                server.send_message(msg)
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"Email sending failed: {str(e)}")

    # Сохраняем уведомление в "базу данных"
    notification = Notification(
        id=next_id,
        email=request.email,
        subject=request.subject,
        message=request.message,
        sent_at=datetime.now()
    )
    notifications_db.append(notification)
    next_id += 1

    return notification

@app.get("/notifications", response_model=List[Notification])
async def get_notifications():
    return notifications_db


@app.get('/health', response_model=ServiceState)
async def health_check():
    """Проверка состояния приложения"""
    return ServiceState(status="OK", message="Application is healthy")


@app.get('/ready')
async def readiness_check():
    """Проверка готовности приложения к работе"""
    return ServiceState(status="OK", message="Application is ready to handle requests")

