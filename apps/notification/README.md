# Сервис уведомлений

Пример микросервиса для отправки email-уведомлений и получения списка сообщений через API,
написанный на Python с использованием FastAPI.

## Запуск

Установка необходимых зависимостей:
```bash
pip3 install -r requirements.txt
````

Старт сервера
```bash
gunicorn -c gunicorn.conf.py main:app
```

## Примеры запросов:

### Отправить уведомление:
```bash
curl -X POST "http://localhost:8000/notifications" \
-H "Content-Type: application/json" \
-d '{"email": "user@example.com", "subject": "Hello", "message": "Test message"}'
```

### Получить список уведомлений:

```bash
curl "http://localhost:8000/notifications"
```