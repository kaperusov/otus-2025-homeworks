
APP_NAME        ?= otus-hw2
APP_VERSION     ?= 2025.05.01
APP_PORT        ?= 8000

DOCKER_REGISTRY ?= kaperusov
DOCKER_IMAGE_V  ?= $(APP_NAME):$(APP_VERSION)
DOCKER_IMAGE_L  ?= $(APP_NAME):latest

.PHONY: all
all: build run health hello logs


# To debuging dockerfile build stage only
go-alpine:
	docker run --rm -it -v .:/app -w /app golang:1.24-alpine3.21

build:
	docker build \
		--build-arg APP_PORT=$(APP_PORT) \
		-t $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_V) \
		-t $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L) \
		.

# Exposed app ports documentation
ports:
	@ echo "Ports that the application listens to inside the container:"
	@ docker inspect --format='{{.NetworkSettings.Ports}}' $(APP_NAME)

run:
	-docker stop $(APP_NAME) 2>/dev/null || true
	docker run --rm \
		--name $(APP_NAME) -d \
		-p $(APP_PORT):$(APP_PORT) \
		$(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L)
	@ echo "-------------------------------"
	docker ps | grep $(APP_NAME)

logs:
	@ echo "-------------------------------"
	docker logs $(APP_NAME) 
	

health: 
	@ echo "-------------------------------"
	curl localhost:$(APP_PORT)/health

hello:
	@ echo "-------------------------------"
	curl localhost:$(APP_PORT)/hello/${USER}

# publish homework
login: 
	docker login --username $(DOCKER_REGISTRY)

push: login
	docker push $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_V)
	docker push $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L)