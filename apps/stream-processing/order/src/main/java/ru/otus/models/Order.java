package ru.otus.controlles.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "number", nullable = false, unique = true)
    private BigDecimal number = BigDecimal.ZERO;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "status")
    private STATUS status;

    @Column(name = "createdAt")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private Long version; // Для оптимистичной блокировки
}
