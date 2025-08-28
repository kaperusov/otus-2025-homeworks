# Проектная работа

Тема: **Интернет-магазин**

Используемые технологии и паттерны:

1. Общий стек технологий: Java, Pyhton, H2, JPA Hibernate
2. Паттерны: API Gateway, REST API, Saga, Transaction orchestration, Idempotency, Observability
3. Перечень используемых инструментов: Kubernetes, Helm, Prometheus, Grafana, Keyclock, Postman, newman 



## Архитектура реализации

Участники процесса

 - **Клиент** - покупатель, инициирующий заказ
 - **OrderService** - оркестратор процесса создания заказа
 - **BillingService** - управление платежами и счетами
 - **WarehouseService** - управление складскими запасами
 - **DeliveryService** - организация доставки
 - **NotificationService** - отправка уведомлений

### Схема аутентификации

```mermaid
sequenceDiagram
    participant Client
    participant GW
    participant Keycloak
    participant Backend

    Note over Client, Backend: Регистрация нового пользователя
    Client->>GW: POST /auth/register
    GW->>Keycloak: Запрос на создание пользователя
    Keycloak-->>GW: Ответ об успешной регистрации
    GW-->>Client: Успешная регистрация

    Note over Client, Backend: Авторизация
    Client->>GW: POST /auth/
    GW->>Keycloak: Запрос аутентификации
    Keycloak-->>GW: JWT токен
    GW->>GW: Сохраняет токен в сессии
    GW-->>Client: Успешная авторизация + cookie сессии

    Note over Client, Backend: Защищённый запрос
    Client->>GW: Запрос к API (с cookie сессии)
    GW->>GW: Проверяет JWT в сессии
    alt Токен валиден
        GW->>Backend: Проксирует запрос с JWT
        Backend-->>GW: Ответ от backend
        GW-->>Client: Ответ клиенту
    else Токен невалиден/отсутствует
        GW-->>Client: 401 Unauthorized
    end

    Note over Client, Backend: Получение профиля
    Client->>GW: GET /auth/profile/me
    GW->>GW: Проверяет JWT в сессии
    GW->>Keycloak: Запрос информации о пользователе
    Keycloak-->>GW: Данные профиля
    GW-->>Client: Информация о профиле

    Note over Client, Backend: Выход из системы
    Client->>GW: POST /auth/logout
    GW->>GW: Удаляет сессию и JWT
    GW->>Keycloak: Инвалидирует токен (опционально)
    Keycloak-->>GW: Подтверждение
    GW-->>Client: Успешный выход
```


### Диаграмма последовательности для ключевого сценария

```mermaid
sequenceDiagram
    participant Client
    participant OrderService
    participant BillingService
    participant WarehouseService
    participant DeliveryService
    participant NotificationService

    Note over Client, BillingService: Подготовка: создание и пополнение счёта
    Client->>BillingService: POST /api/v1/billing/accounts
    Note right of BillingService: Создание платёжного аккаунта
    BillingService-->>Client: {accountId: acc123, status: ACTIVE}
    
    Client->>BillingService: POST /api/v1/billing/deposit
    Note right of BillingService: Пополнение баланса
    BillingService-->>Client: {success: true, newBalance: 5000}
    
    Note over Client, DeliveryService: Основной процесс создания заказа
    Client->>OrderService: POST /api/v1/order
    Note right of OrderService: Создание заказа со статусом NEW
    
    OrderService->>BillingService: POST /api/v1/billing/withdraw
    BillingService-->>OrderService: {success: true, transactionId: xxx}
    Note right of OrderService: Списание средств со счёта пользователя
    
    BillingService->>NotificationService: POST /api/v1/notifications/payment
    Note right of NotificationService: Уведомление о списании средств
    NotificationService-->>BillingService: {notificationId: notif123}
    
    OrderService->>WarehouseService: POST /api/v1/warehouse/reserve
    WarehouseService-->>OrderService: {success: true, reservationId: yyy}
    Note right of OrderService: Резервирование товара на складе
    
    OrderService->>DeliveryService: POST /api/v1/deliveries/reserve
    DeliveryService-->>OrderService: {success: true, deliveryId: zzz}
    Note right of OrderService: Резервирование доставки
    
    OrderService->>OrderService: Обновление статуса заказа на CONFIRMED
    OrderService-->>Client: 201 CREATED с данными заказа
```

На данной схеме представлен полный цикл обработки заказа в системе электронной коммерции.

Процесс начинается с подготовки: пользователь создает платёжный аккаунт и пополняет баланс.

При оформлении заказа система последовательно выполняет критически важные операции: 
- списание средств с резервированием платежа, 
- автоматическую отправку уведомления о списании, 
- резервирование товара на складе 
- организацию доставки

Каждый этап выполняется строго после успешного завершения предыдущего, что обеспечивает транзакционную целостность процесса. 
Финализация заказа происходит только после подтверждения всех операций, гарантируя клиенту надёжность выполнения его заказа.

Схема демонстрирует слаженное взаимодействие шести независимых сервисов, 
каждый из которых отвечает за свою зону ответственности в рамках единого бизнес-процесса.


### Описание межсервисного взаимодействия 


### Схема хранения данных

У каждого сервиса своя БД, схемы которых представлены ниже. 

В качестве СУБД используется inmemory DB H2, просто для упрощения разработки 
и скорости разворачивания. Но переключение на любую другую реализацию 
реляционной БД не вызывает проблем, так как всё взаимодействие с хранилищем 
просиходит через ORM на базе Hibernate. 

#### Billing Service

```mermaid
erDiagram
    ACCOUNTS {
        bigint id PK "Идентификатор (ID)"
        uuid user_id UK "ID пользователя (уникальный)"
        varchar email UK "Email (уникальный)"
        decimal balance "Баланс счёта"
        bigint version "Версия для оптимистичной блокировки"
    }

    TRANSACTIONS {
        uuid id PK "Идентификатор транзакции (UUID)"
        bigint account_id FK "Ссылка на счёт"
        decimal amount "Сумма транзакции"
    }

    ACCOUNTS ||--o{ TRANSACTIONS : contains
```

#### Delivery Service
```mermaid
erDiagram
    DELIVERY {
        uuid id PK "Идентификатор доставки (UUID)"
        uuid order_id "ID заказа"
        timestamp created_at "Дата и время создания"
    }

    DELIVERY_RESERVATIONS {
        uuid id PK "Идентификатор резервирования (UUID)"
        uuid order_id "ID заказа"
        timestamp preferred_time_slot "Предпочитаемый слот времени"
        varchar address "Адрес доставки"
    }

    DELIVERY_RESERVATIONS ||--o{ DELIVERY : contains
```

#### Order Service
```mermaid
erDiagram
    ORDERS {
        uuid id PK "ID заказа"
        decimal price "Сумма заказа"
        uuid user_id "ID пользователя"
        varchar number UK "Номер заказа"
        varchar description "Описание"
        varchar status "Статус"
        varchar error_message "Сообщение об ошибке"
        timestamp created_at "Дата создания"
        timestamp updated_at "Дата обновления"
        bigint version "Версия для блокировки"
    }

    ORDER_ITEMS {
        bigint id PK "ID позиции"
        varchar name "Название товара"
        uuid product_id "ID товара"
        int quantity "Количество"
        decimal price "Цена за единицу"
        timestamp created_at "Дата создания"
        uuid order_id FK "ID заказа"
    }

    ORDERS ||--o{ ORDER_ITEMS : contains
```

#### Warehouse Service
```mermaid
erDiagram
    STOCK_RESERVATIONS {
        uuid id PK "ID резервирования"
        uuid order_id "ID заказа"
        timestamp created_at "Дата создания"
        varchar status "Статус резервирования"
    }

    STOCKS {
        uuid id PK "ID записи"
        uuid product_id UK "ID товара (уникальный)"
        int total_quantity "Общее количество"
        int reserved_quantity "Зарезервированное количество"
        uuid reservation_id FK "ID резервирования"
    }

    STOCK_RESERVATIONS ||--o{ STOCKS : "contains"
```

### Saga. Ключевые аспекты реализации

**Order Service (Orchestrator)**

 - Управляет workflow всей саги
 - Выполняет последовательные вызовы к другим сервисам
 - Обрабатывает ошибки и запускает компенсирующие транзакции
 - Обновляет статус заказа на каждом этапе

**Сервис-участники (Participants)**

 - Billing Service: Сервис проведения платежных операций
 - Warehouse Service: Резервирование товаров на складе
 - Delivery Service: Бронирование временного слота доставки
 - Notification Service: Сервис уведомления пользователя о состоянии заказа
 
**Механизм компенсации**

Каждый сервис предоставляет:
 - Основную операцию (например, /reserve)
 - Компенсирующую операцию (например, /reserve/cancel/{id})


### DTO для межсервисного взаимодействия

```java
@Data
public class SagaStepResult {
    private boolean success;
    private String message;
    private UUID transactionId; // ID для компенсации
    private Object data;
}
```

### Жизненный цикл заказа

```java
public enum OrderStatus {
    NEW,             // Новый заказ (начало саги)
    PROCESSING,      // В обработке
    PAID,            // Оплачен
    ITEMS_RESERVED,  // Товары зарезервированы
    DELIVERY_BOOKED, // Доставка забронирована
    CONFIRMED,       // Заказ обработан
    FAILED           // Ошибка при обработке
}
```

### Механизм отката

```java
SagaStepResult warehouseResult = reserveItems(orderId, request.getItems());
log.debug( "2. Warehouse reservation result: {}", warehouseResult);
if (warehouseResult.isSuccess()) {
    updateOrderStatus(orderId, OrderStatus.ITEMS_RESERVED, null);
} else {
    cancelPayment(orderId, paymentResult.getTransactionId());
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Warehouse reservation failed: " + warehouseResult.getMessage());
}

SagaStepResult deliveryResult = reserveDelivery(orderId, request.getDeliveryInfo());
log.debug( "3. Delivery reservation result: {}", deliveryResult);
if (deliveryResult.isSuccess()) {
    updateOrderStatus(orderId, OrderStatus.DELIVERY_BOOKED, null);
} else {
    cancelPayment(orderId, paymentResult.getTransactionId());
    cancelReservation(orderId, warehouseResult.getTransactionId());
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delivery reservation failed: " + deliveryResult.getMessage());
}
```


### Гарантии согласованности

1. **Атомарность на уровне бизнес-процесса** - либо все шаги успешны, либо все откатываются
2. **Идемпотентность операций** - повторные вызовы компенсации безопасны
3. **Транзакционность данных** - каждый сервис управляет своей БД атомарно
4. **Наблюдаемость** - каждый этап логируется и может быть аудирован


### Идемпотентность 

Реализован механизм гарантированной идемпотентности на основе уникального ключа, который клиент передает при каждом запросе создания заказа. 

Сервер кэширует результаты обработки запросов и при повторных запросах с тем же ключом возвращает результат из кэша без повторного выполнения бизнес-логики.

Дополнительно проверяется наличие существующего заказа в БД по бизнес-правилам: 
тот же пользователь + похожие товары + пятиминутный интервал времени, в котором похожий заказ мог бы быть сделан.

1. В случае, если заказ успешно созадётся в первый раз, сервер возвращает код 201 (CREATED). 
2. При нахождении запроса на заказ в кеше по ключу, сервер вернёт код 200 (OK). То есть это показывает, что на сервере не был создан новый ресурс (в виде записи в БД). 
3. А в случае нахождения похожего заказа в БД, сервер ответит ошибкой 409 (CONFLICTED), но всё равно вернёт найденный заказ в теле ответа.


