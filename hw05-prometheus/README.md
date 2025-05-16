# Prometheus. Grafana // ДЗ 

Инструментировать сервис из прошлого задания метриками в формате Prometheus с помощью библиотеки для вашего фреймворка и ЯП.

## Краткое описание выполненной работы: 

1. Добавлены метриками в формате Prometheus [в мой проект](../hw04-helm/internal/prometheus.go)

2. Проверка, что необходимый metrics-server addon для minikube включен:
```bash
minikube addons list

# if needed
minikube addons enable metrics-server
```



2. Установка Prometheus и Grahana:

```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm -n otus install stack prometheus-community/kube-prometheus-stack -f prometheus/values.yaml
```

Пароль к Grahana: 
```bash
kubectl get secret --namespace otus stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```

Для передачи метрик из приложения в Prometheus в Kubernetes необходим специальный ресурс: `ServiceMetric` 
Но для его работы требоваться установка kube-prometheus-stack (включает оператор)
```bash
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace otus \
  --create-namespace
```

Проверка
```bash
kubectl -n otus get crd | grep servicemonitors
```

После чего необходимо применить манифест `service-monitor.yaml`:
```bash
kubectl apply -f service-monitor.yaml
```

Для входа в UI
```bash
export POD_NAME=$(kubectl get pods --namespace otus -l "app.kubernetes.io/name=prometheus,app.kubernetes.io/instance=prom" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace otus port-forward $POD_NAME --address 0.0.0.0 9090
```

Метрики приложения:

    http://localhost:8080/prometheus



5. Генерим трафик: 

    hey -z 5m -q 5 -m GET -H "Accept: text/html" http://127.0.0.1:8080