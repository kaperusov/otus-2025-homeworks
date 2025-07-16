package ru.otus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.controlles.models.Order;

import java.util.List;
import java.util.UUID;

public interface OrderRepository  extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
}
