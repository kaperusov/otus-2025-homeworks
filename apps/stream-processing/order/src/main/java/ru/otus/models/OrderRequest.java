package ru.otus.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class OrderRequest {

    private UUID userId;
    private List<OrderItem> items;
    private PaymentInfo paymentInfo;
    private DeliveryInfo deliveryInfo;

    @Data
    public static class OrderItem {
        private String name;
        private UUID productId;
        private int quantity;
        private BigDecimal price;
    }

    @Data
    public static class PaymentInfo {
        private String cardNumber;
        private String expiryDate;
        private String cvv;
        private BigDecimal amount;
    }

    @Data
    public static class DeliveryInfo {
        private String address;
        private LocalDateTime preferredTimeSlot;
    }
}