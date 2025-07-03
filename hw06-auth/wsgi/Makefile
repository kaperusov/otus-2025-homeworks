ENV_FILE=.env
ifneq ("$(wildcard $(ENV_FILE))","")
    include $(ENV_FILE)
endif

APP_NAME        ?= wsgi
APP_VERSION     ?= $(shell python3 wsgi.py -v)
PROFILE         ?= dev

DOCKER_REGISTRY ?= unknown:5000
DOCKER_IMAGE_V  ?= $(DOCKER_REGISTRY)/$(APP_NAME):$(APP_VERSION)
DOCKER_IMAGE_L  ?= $(DOCKER_REGISTRY)/$(APP_NAME):latest

info:
	@ echo "-----------------------------------------"
	@ echo "APP_NAME           : $(APP_NAME)"
	@ echo "APP_VERSION        : $(APP_VERSION)"
	@ echo ""
	@ echo "DOCKER_REGISTRY    : $(DOCKER_REGISTRY)"
	@ echo "DOCKER_IMAGES      : $(DOCKER_IMAGE_V)"
	@ echo "                   : $(DOCKER_IMAGE_L)"
	@ echo "-----------------------------------------"

docker-image: info
	docker build . \
      --no-cache \
      --file Dockerfile \
		--tag "$(DOCKER_IMAGE_V)" \
		--tag "$(DOCKER_IMAGE_L)"

docker-push:
	docker push "$(DOCKER_IMAGE_V)"
	docker push "$(DOCKER_IMAGE_L)"

docker-run:
	docker run \
	  -p 5000:5000 \
	  -v ./config.ini:/app/config.ini \
	   "$(DOCKER_IMAGE_V)"

docker-run-dev:
	docker run \
	  -p 5000:5000 \
	  -v ./config-$(PROFILE).ini:/app/config.ini \
	   "$(DOCKER_IMAGE_L)"