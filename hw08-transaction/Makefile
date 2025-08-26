
NAMESPACE := otus
RELEASES  := billing notification order warehouse delivery
DRY_RUN   := 
#--dry-run

.PHONY: $(RELEASES)
$(RELEASES):
	@if [ -f charts/$@/values-private-data.yaml ]; then \
		echo "Using private values file..."; \
		helm -n $(NAMESPACE) upgrade --install $@ charts/$@/ \
			--values charts/$@/values.yaml \
			--values charts/$@/values-private-data.yaml \
			$(DRY_RUN); \
	else \
		echo "Private values file not found, installing without it..."; \
		helm -n $(NAMESPACE) upgrade --install $@ charts/$@/ \
			--values charts/$@/values.yaml \
			$(DRY_RUN); \
	fi