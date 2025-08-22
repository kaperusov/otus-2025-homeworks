package ru.otus.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class ReserveItemRequest {
    private UUID orderId;
    private List<OrderItem> items;

    @Data
    public static class OrderItem {
        private UUID productId;
        private int quantity;
    }
}