# Идемпотентость и коммутативность API в HTTP и очередях // ДЗ 

Данная работа демонстрирует реализацию идемпотентности в методе создания заказа

## Описание

Реализован механизм гарантированной идемпотентности на основе уникального ключа, 
который клиент передает при каждом запросе создания заказа. Сервер кэширует результаты обработки 
запросов и при повторных запросах с тем же ключом возвращает результат из кэша без повторного 
выполнения бизнес-логики. 

Дополнительно проверяется наличие существующего заказа в БД по бизнес-правилам: 
тот же пользователь + похожие товары + пятиминутный интервал времени, в котором похожий заказ мог бы быть сделан

В случае, если заказ успешно созадётся в первый раз, сервер возвращает код **201 (CREATED)**. 
При нахождении запроса на заказ в кеше по ключу, сервер вернёт код **200 (OK)**. То есть мы не созадли новый ресурс в БД.
А в случае нахождения похожего заказа в БД, сервер ответит ошибкой **409 (CONFLICTED)** и вернёт 
найдейнный заказ в теле ответа. 

## Реализация на серверной стороне

```java
// Проверяем идемпотентность по ключу (краткосрочная)
if (request.getIdempotencyKey() != null) {
    Object cachedResult = idempotencyService.getCachedResult(request.getIdempotencyKey());
    if (cachedResult != null) {
        log.info("Returning cached result for idempotency key: {}", request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.OK).body(cachedResult);
    }

    if (idempotencyService.isDuplicateRequest(request.getIdempotencyKey())) {
        log.warn("Duplicate request with idempotency key: {}", request.getIdempotencyKey());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body("Duplicate request. Use unique idempotency key.");
    }
}

// Проверяем дубликаты по БД (долгосрочная)
Optional<Order> duplicateOrder = orderService.findDuplicateOrder(request);
if (duplicateOrder.isPresent()) {
    return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(duplicateOrder);
}

try {
    Order order = orderService.createOrder(request);
    // Сохраняем результат для будущих дубликатов
    if (request.getIdempotencyKey() != null) {
        idempotencyService.storeRequestResult(request.getIdempotencyKey(), order);
    }

    return ResponseEntity.status(HttpStatus.CREATED).body(order);
}
...
```

## Установка приложения

Так как работа опирается на предыдущие ДЗ, то для полного восспроизведения, необходимо установить сервисы из моих прошлых работ:

- [../hw06-auth](../hw06-auth/README.md)
- [../hw07-restful](../hw07-restful/README.md)
- [../hw08-transaction](../hw08-transaction/README.md)

Для простоты развёртывания я сделал копию helm чарта для сервиса order и пересобарл соответсвующего приложение (версия 3.0.0), 
поэтому из этой папки ДЗ нужно выполнить всего лишь одну команду: 

```shell
helm -n otus install order charts/order/ --values charts/order/values.yaml
```

## Тесты Postman

[Коллекция запросов к API для postman](postman_collection.json)

Запуск тестов можно выполнить командой 
```bash
newman run postman_collection.json
```

Метод POST /api/v1/order в тестах вызывается 3 раза. 

В первый раз `idempotencyKey` генерируется в секции `Pre-request` для теста и проверяется результат на код 201.
Во втором вызове, берётся предыдущее значение `idempotencyKey` и повторяется запрос. Но тест проверяет результат на код 200.
В последний раз запрос выполняется без ключа `idempotencyKey` и сервер вернёт код 409 - это нормально, сервер нашёл 
дубликат и вернул его в качестве ответа, но с предупреждающей ошибкой о конфликте бизнес логики. 