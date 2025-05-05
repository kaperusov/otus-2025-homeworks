
APP_NAME        ?= otus-hw04-user-crud
APP_VERSION     ?= 2025.05.03
APP_PORT        ?= 8080

DOCKER_REGISTRY ?= kaperusov
DOCKER_IMAGE_V  ?= $(APP_NAME):$(APP_VERSION)
DOCKER_IMAGE_L  ?= $(APP_NAME):latest

.PHONY: all
all: build docker-image


# To debuging dockerfile build stage only
go-alpine:
	docker run --rm -it -v .:/app -w /app golang:1.24-alpine3.21

.PHONY: swag
swag:
	~/go/bin/swag init --generalInfo main.go --dir . --output docs --parseDependency

build: swag
	CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o ./bin/app

docker-image:
	docker build \
		-t $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_V) \
		-t $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L) \
		.

run:
	-docker stop $(APP_NAME) 2>/dev/null || true
	docker run --rm \
		--name $(APP_NAME) -d \
		-p $(APP_PORT):$(APP_PORT) \
		$(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L)
	@ echo "-------------------------------"
	docker ps | grep $(APP_NAME)



# publish homework
login: 
	docker login --username $(DOCKER_REGISTRY)

docker-push: login
	docker push $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_V)
	docker push $(DOCKER_REGISTRY)/$(DOCKER_IMAGE_L)


NAMESPACE := otus 
DRY_RUN   := 
#--dry-run

helm-values:
	@ /bin/bash ./setup.sh

helm-install-all: \
	helm-values \
	helm-install-db \
	helm-install-app 

helm-install-app:
	helm upgrade --install app --create-namespace --namespace $(NAMESPACE) $(DRY_RUN) \
		./charts/app \
		--values charts/app/values.yaml \
		--values charts/db/values.yaml

helm-install-db:
	helm install db --create-namespace --namespace $(NAMESPACE) $(DRY_RUN) \
		oci://registry-1.docker.io/bitnamicharts/postgresql \
		--values charts/db/values.yaml