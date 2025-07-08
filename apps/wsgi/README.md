# Simple API Gateway

## Структура проекта

```
api-gateway/
├── app/
│   ├── __init__.py          # Инициализация приложения Flask
│   ├── config*.py           # Конфигурация приложения
│   ├── logger*.py           # Журналирование
│   ├── routes/
│   │   ├── __init__.py
│   │   ├── auth.py          # Маршруты аутентификации
│   │   ├── api.py           # Основные API маршруты (прокси)
│   │   └── state.py         # Методы отвечающие за состояние приложения
│   ├── services/
│   │   ├── __init__.py
│   │   ├── keyclak_service.py  # Логика работы с Keycloak
│   │   ├── backend_client.py   # Клиент для бэкенд-сервиса
│   │   ├── session_manager.py  # Менеджер сессий
│   │   └── utils.py            # Вспомогательные утилиты
│   ├── templates/              # Шаблоны для форм аутентификации
│   │   ├── login.html
│   │   ├── register.html
│   │   └── ...
│   └── static/              # Статические файлы (CSS, JS)
│       ├── css/
│       └── js/
├── requirements.txt         # Зависимости
├── .env                     # Переменные окружения (используется только в docker-compose.yml)
├── .gitignore
├── Dockerfile               # Инструкция для контейнеризации прилоежния
├── docker-compose.yml       # Для локальной разработки
├── postman_collection.json  # Набор тестовых сценариев для Postman
├── Makefile                 # Инструкция для сборки приложения через утилиту GNU Make
└── wsgi.py                  # Точка входа для запуска
```


## Сборка и запуск на локальной машине:

И выполняем следующие команды:
```shell
python3 -m venv .venv
source .venv/bin/activate
pip3 install -r requirements.txt
```
Запуск приложения

```shell
gunicorn --bind 0.0.0.0:5000 wsgi:application
```

### Сборка docker образа:

Ручная сборка
```shell
export DOCKER_REGISTRY=$(grep -oP '(?<=^DOCKER_REGISTRY=)\w+$' .env)
docker build . \
  --no-cache \
  --file Dockerfile \
  --tag "${DOCKER_REGISTRY}/wsgi:$(python3 wsgi.py -v)" \
  --tag "${DOCKER_REGISTRY}/wsgi:latest" 
```
Где `${DOCKER_REGISTRY}` - это имя репозитория, которое берётся из `.env` файла (необходимо предварительно создать),
версия собираемого образа - это версия приложения, которая находится в файле `app/_version.py` 

Запуск контейнера:
```bash
docker run \
  -p 5000:5000 \
  -v ./config.ini:/app/config.ini \
   "${DOCKER_REGISTRY}/wsgi:latest"
```

### Публикация в репозиторий:
```bash
docker push "${DOCKER_REGISTRY}/wsgi:$(python3 wsgi.py -v)"
docker push "${DOCKER_REGISTRY}/wsgi:latest"
```

### Makefile

Для большего удобства можно воспользоваться утилитой GNU Make:

```bash
# Сборка образа
make docker-image # для сборки образа
make docker-run   # для запуска
make docker-push  # для публикации в репозитории
```

