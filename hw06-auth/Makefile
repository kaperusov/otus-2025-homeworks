PRIVATE_VALUES := charts/wsgi/values-private-data.yaml

all: $(PRIVATE_VALUES)

define GET_PASSWORD
$(shell kubectl get secret --namespace otus kc-keycloak -o jsonpath="{.data.admin-password}" | base64 -d)
endef

$(PRIVATE_VALUES):
	@mkdir -p charts/wsgi
	@echo "keycloak:" > $@
	@echo "  clientId: otus" >> $@
	@echo "  clientSecret: " >> $@
	@echo "  adminUsername: user" >> $@
	@echo "  adminPassword: $(call GET_PASSWORD)" >> $@
	@echo "Файл $@ успешно создан"

clean:
	rm -f $(PRIVATE_VALUES)

wsgi:
	helm -n otus upgrade --install wsgi charts/wsgi/ \
		--values charts/wsgi/values.yaml \
		--values charts/wsgi/values-private-data.yaml

.PHONY: wsgi clean