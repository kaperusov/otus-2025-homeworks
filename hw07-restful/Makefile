
NAMESPACE := otus
RELEASES  := billing notification order


.PHONY: $(RELEASES)
$(RELEASES):
	@if [ -f charts/$@/values-private-data.yaml ]; then \
		echo "Using private values file..."; \
		helm -n $(NAMESPACE) upgrade --install $@ charts/$@/ \
			--values charts/$@/values.yaml \
			--values charts/$@/values-private-data.yaml; \
	else \
		echo "Private values file not found, installing without it..."; \
		helm -n $(NAMESPACE) upgrade --install $@ charts/$@/ \
			--values charts/$@/values.yaml; \
	fi