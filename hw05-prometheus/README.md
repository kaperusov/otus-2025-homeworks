# Prometheus. Grafana // ДЗ 

Инструментировать сервис из прошлого задания метриками в формате Prometheus с помощью библиотеки для вашего фреймворка и ЯП.

## Краткое описание выполненной работы: 


### 1. Установка приложения 

Используется helm из предыдущего ДЗ (hw04-helm), и новый docker образ (kaperusov/otus-app-user-crud:hw05) 
в котором [добавлены метрики prometheus](../hw04-helm/internal/prometheus.go):

```bash
helm upgrade --install app --create-namespace --namespace otus \
  ../hw04-helm/charts/app/ \
  --values charts/app/values.yaml
```

Метрики доступны по адресу:

  http://arch.homework/prometheus
  

### 2. Установка Prometheus и Grafana

Для начала надо проверить, что необходимый metrics-server addon для minikube включен:
```bash
minikube addons list
```
При необходимости включить: 
```bash
minikube addons enable metrics-server
```

Для Prometheus и Grahana, я воспользовался helm чартом, который устанавливает весь стек: Prometheus + ServiceMetric (оператор) + Grafana
```bash
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update 
helm -n otus install stack prometheus-community/kube-prometheus-stack -f prometheus/values.yaml
```

Для связи метрик приложения к Prometheus необходимо создать ServiceMonitor, 
для этого применяем следующий манифест:
```bash
kubectl apply -f service-monitor.yaml
```

Для входа в UI Prometheus:
```bash
export PROM=$(kubectl get pods --namespace otus -l "app.kubernetes.io/name=prometheus,app.kubernetes.io/instance=stack-kube-prometheus-stac-prometheus" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace otus port-forward $PROM --address 0.0.0.0 9090
```

Для входа в UI Grahana:
```bash
export GRAF=$(kubectl get pods --namespace otus -l "app.kubernetes.io/name=grafana,app.kubernetes.io/instance=stack" -o jsonpath="{.items[0].metadata.name}")
kubectl --namespace otus port-forward $GRAF --address 0.0.0.0 3000
```

Пароль к Grahana: 
```bash
kubectl get secret --namespace otus stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode ; echo
```


### 3. Генерим трафик: 

    hey -z 5m -q 5 -m GET -H "Accept: text/html" http://arch.homework/api/v1/users/1


### 4. PromQL запросы 

1. Расчёт Latency (response time) с квантилями по 0.5, 0.95, 0.99, max

```promql
# Медиана (50-й перцентиль)
histogram_quantile(0.5, sum(rate(http_response_time_seconds_bucket{path=~"/api/v1/.*"}[1m])) by (le, method, path))

# 95-й перцентиль
histogram_quantile(0.95, sum(rate(http_response_time_seconds_bucket{path=~"/api/v1/.*"}[1m])) by (le, method, path))

# 99-й перцентиль
histogram_quantile(0.99, sum(rate(http_response_time_seconds_bucket{path=~"/api/v1/.*"}[1m])) by (le, method, path))

# Максимальная задержка
histogram_quantile(1.0, sum(rate(http_response_time_seconds_bucket{path=~"/api/v1/.*"}[1m])) by (le, method, path))
```


2. RPS (Requests Per Second) - средняя скорость роста метрики за определённый промежуток времени

```promql
rate(http_response_time_seconds_count{path=~"/api/v1/.*"}[1m])
```

3. Error Rate - количество 500ых ответов

```promql
rate(http_requests_total{status_code=~"5.."}[1m])
```

Dashboard для Grafana по этим запросам в файле [grafana/01-api-dashboard.json](grafana/01-api-dashboard.json)
