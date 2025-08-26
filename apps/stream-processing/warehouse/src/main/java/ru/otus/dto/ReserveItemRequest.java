package ru.otus.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class ReserveItemRequest {
    private UUID orderId;
    private List<OrderItem> items;

    @Data
    public static class OrderItem {
        private String name;
        private UUID productId;
        private int quantity;
        private BigDecimal price;
    }
}