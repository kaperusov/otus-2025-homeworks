package ru.otus.models;

public enum OrderStatus {
    NEW,          // Новый заказ
    PROCESSING,   // В обработке
    PAID,         // Оплачен
    SHIPPED,      // Отправлен
    DELIVERED,    // Доставлен
    CANCELLED,    // Отменен
    CONFIRMED,    // Заказ обработан
    FAILED        // Ошибка при обработке
}
