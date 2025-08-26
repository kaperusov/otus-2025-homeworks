package ru.otus.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.otus.dto.DeliveryReserveRequest;
import ru.otus.dto.SagaStepResult;
import ru.otus.service.DeliveryService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping("/reserve")
    public SagaStepResult reserveDelivery(@RequestBody DeliveryReserveRequest request) {
        try {
            UUID deliveryId = deliveryService.reserveDelivery(
                    request.getOrderId(),
                    request.getAddress(),
                    request.getPreferredTimeSlot()
            );
            return new SagaStepResult(true, "Delivery reserved", deliveryId, null);
        } catch (Exception e) {
            return new SagaStepResult(false, e.getMessage(), null, null);
        }
    }

    @PostMapping("/reserve/cancel/{deliveryId}")
    public ResponseEntity<String> cancelDeliveryReservation(
            @PathVariable("deliveryId") UUID deliveryId) {
        deliveryService.cancelReservation(deliveryId);
        return ResponseEntity.ok("Delivery reservation cancelled");
    }
}
