# Домашнее задание
Основы работы с Kubernetes (Часть 2)


## Подготовка 

### Установка Minikube

```bash
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube && rm minikube-linux-amd64

# При необходимости указать в качестве двайвера Docker (обычно он и сам по умолчанию такой)
minikube config set driver docker
```
Подробнее об устнавке и настройке см. [документацию](https://minikube.sigs.k8s.io/docs/start/)

Дополнительно в Minikube необходимо включить Ingress-контроллер 
```bash
minikube addons enable ingress

# проверка, что котроллер работает
kubectl get pods -n ingress-nginx
```


## Выполнение ДЗ

**Шаг 1.** 

Исходный код сервиса располагается в папке [hw02-docker](../hw02-docker/)

**Шаг 2.**

Oбраз в [dockerhub](https://hub.docker.com/repository/docker/kaperusov/otus-hw2)

**Шаг 3.**

Применение всех манифестов можно выполнить одной командой:
```bash
kubectl apply -f .
```

Чтобы хост `arch.homework`, указанный в ингрессе, резолвился на локальной машине 
нужно вписать IP адрес миникуба в /etc/hosts: 
```bash
echo "$(minikube ip) arch.homework" | sudo tee -a /etc/hosts
```

**Шаг 4.**

Проверка доступности приложения:
```bash
curl -s http://arch.homework/health | jq
```

**Задание со звездочкой**

Для реализации правила переадрсации с /otusapp/{student name}/* на arch.homework/health
необходимо изменить `pathType` c `Prefix` на `ImplementationSpecific` и включить две аннотации
разрешающую regex + rewrite: 

```yaml
annotations:
  nginx.ingress.kubernetes.io/rewrite-target: /$2 
  nginx.ingress.kubernetes.io/use-regex: "true"
```

После чего, сам путь можно обработать таким выражением: `/otusapp/([^/]+)/(.*)`,
что в nginx контроллере будет интерпретировано как 
```
location ~* ^/otusapp/([^/]+)/(.*) {
    rewrite ^/otusapp/([^/]+)/(.*) /$2 break; # режет /otusapp/{student} оставляя хвост
    proxy_pass http://hw-svc.otus.svc.cluster.local;
}
```

Таким образом, правило rewrite будет работать следующим образом:

$1 — это ([^/]+) → имя студента.
$2 — это (.*) → health (хвост url)
