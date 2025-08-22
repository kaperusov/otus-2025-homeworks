package ru.otus.controlles.models;

public enum STATUS {
    NEW,          // Новый заказ
    PROCESSING,   // В обработке
    PAID,         // Оплачен
    SHIPPED,      // Отправлен
    DELIVERED,    // Доставлен
    CANCELLED,    // Отменен
    REFUNDED,     // Возврат оформлен
    FAILED        // Ошибка при обработке
}
