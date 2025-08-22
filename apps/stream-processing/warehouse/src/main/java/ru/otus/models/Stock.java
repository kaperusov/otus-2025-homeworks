package ru.otus.models;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "stocks")
@Data
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @ManyToOne
    private Reservation reservation;

    public int getAvailableQuantity() {
        return totalQuantity - reservedQuantity;
    }
}