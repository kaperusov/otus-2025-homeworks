ENV_FILE=.env
ifneq ("$(wildcard $(ENV_FILE))","")
    include $(ENV_FILE)
endif

DOCKER_REGISTRY ?= kaperusov
MODULES         := billing notification order warehouse delivery

print-versions:
	@for module in $(MODULES); do \
		echo "$$module version: $$(cd $$module && mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr -d '[:space:]')"; \
	done

.PHONY: $(MODULES)
$(MODULES):
	$(eval VERSION := $(shell cd $@ && mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr -d '[:space:]' | sed 's/\x1b\[[0-9;]*m//g'))
	docker build --target $@-app \
		--tag $(DOCKER_REGISTRY)/$@:$(VERSION) \
		--tag $(DOCKER_REGISTRY)/$@:latest \
		.

build-all: $(MODULES)

push-all:
	for module in $(MODULES); do \
		VERSION=$$(cd $$module && mvn help:evaluate -Dexpression=project.version -q -DforceStdout | tr -d '[:space:]' | sed 's/\x1b\[[0-9;]*m//g'); \
		docker push $(DOCKER_REGISTRY)/$$module:$$VERSION; \
		docker push $(DOCKER_REGISTRY)/$$module:latest; \
	done