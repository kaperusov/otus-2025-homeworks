
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
		../hw04-helm/charts/app/ \
		--values charts/app/values.yaml
		

helm-install-db:
	helm install db --create-namespace --namespace $(NAMESPACE) $(DRY_RUN) \
		oci://registry-1.docker.io/bitnamicharts/postgresql \
		--values ../hw04-helm/charts/db/values.yaml



# helm upgrade --install app --create-namespace --namespace otus \
# 		../hw04-helm/charts/app/ \
# 		--values charts/app/values.yaml \
# 		--values ../hw04-helm/charts/db/values.yaml