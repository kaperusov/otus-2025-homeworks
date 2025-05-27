# Simple IAM Proxy Server

## Сборка и запуск на локальной машине:

Переходим в папку проекта
```shell
cd iam-proxy
````
И выполняем следующие команды:
```shell
python3 -m venv venv
source venv/bin/activate
pip3 install -r requirements.txt
```
Запуск приложения делается командой

```shell
python3 front-app.py [--profile=<ARG>]
```
Где `ARG` - условное имя профиля, это постфикс по которому приложение будет искать
конфигурационный файл используя маску: `config-<ARG>.ini`.
Если ключ `--profile` не передавать, то по умолчанию приложение будет использоваться 
файл `config.ini`


## Сборка docker образа:

Ручная сборка
```shell
export DOCKER_REGISTRY=$(grep -oP '(?<=^DOCKER_REGISTRY=)\w+$' .env)
docker build . \
  --no-cache \
  --file Dockerfile \
  --tag "${DOCKER_REGISTRY}/iam-proxy:$(cat .version)" \
  --tag "${DOCKER_REGISTRY}/iam-proxy:latest" 
```
Где `${DOCKER_REGISTRY}` - это имя вашего репозитория, берётся из `.env` файла (необходимо предварительно создать),
версия собираемого образа - это версия приложения, которая находится в файле `.version` 

Запуск контейнера:
```bash
docker run \
  -p 5000:5000 \
  -v ./config.ini:/app/config.ini \
   "${DOCKER_REGISTRY}/iam-proxy:latest"
```

Для быстрого старта, одной командой, или если не хочется тагировать создаваемый образ, то можно воспользоваться такой 
командой:

```shell
docker run --rm --name iam \
  -p 5000:5000 \
  -v $(pwd)/config-<profile>.ini:/app/config.ini \
  $(docker build -q .)
```
Или взять уже готовый образ из репозитория: 
```shell
docker run --rm --name iam \
  -p 5000:5000 \
  -v $(pwd)/config-<profile>.ini:/app/config.ini \
  "${DOCKER_REGISTRY}/iam-proxy:latest"
```


### Публикация в репозитории: 
```bash
docker push "${DOCKER_REGISTRY}/iam-proxy:$(cat .version)"
docker push "${DOCKER_REGISTRY}/iam-proxy:latest"
```

`<VERSION>` - версия приложения. Фиксируется в файле `.version`. 
Поэтому, для простоты, лучше воспользоваться утилитой gnu make:

```shell
make build    # сборка образа
make run      # запуск, при необходимости
make pulish   # публикация в репозитории
```

Запуск в контейнере без сборки образа: 
```bash
docker run -it -p 5000:5000 \
  --mount type=bind,source=.,target=/app \
  --entrypoint '/bin/sh' python:3.11.7-slim -c 'cd /app && pip3 install -r requirements.txt && python3 front-app.py'
```

