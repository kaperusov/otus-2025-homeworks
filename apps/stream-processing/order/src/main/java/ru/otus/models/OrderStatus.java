package ru.otus.models;

public enum OrderStatus {
    NEW,             // Новый заказ (начало саги)
    PROCESSING,      // В обработке
    PAID,            // Оплачен
    ITEMS_RESERVED,  // Товары зарезервированы
    DELIVERY_BOOKED, // Доставка забронирована
    CONFIRMED,       // Заказ обработан
    FAILED           // Ошибка при обработке
}
