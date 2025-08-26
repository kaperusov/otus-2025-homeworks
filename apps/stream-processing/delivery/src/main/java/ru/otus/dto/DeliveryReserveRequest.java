package ru.otus.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class DeliveryReserveRequest {
    private UUID orderId;
    private String address;
    private LocalDateTime preferredTimeSlot;
}
