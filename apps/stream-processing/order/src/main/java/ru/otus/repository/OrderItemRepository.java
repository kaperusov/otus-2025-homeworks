package ru.otus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.models.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
