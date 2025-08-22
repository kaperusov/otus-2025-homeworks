package ru.otus.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.otus.dto.ReserveItemRequest;
import ru.otus.dto.ReserveResult;
import ru.otus.service.WarehouseService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping("/reserve")
    public ResponseEntity<ReserveResult> reserveItems(
            @Valid @RequestBody ReserveItemRequest request) {

        ReserveResult result = warehouseService.reserveItems(request);

        if (!result.isSuccess()) {
            return ResponseEntity.badRequest().body(result);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reserve/cancel/{reservationId}")
    public ResponseEntity<String> cancelReservation(
            @PathVariable("reservationId") UUID reservationId) {

        try {
            boolean cancelled = warehouseService.cancelReservation(reservationId);

            if (cancelled) {
                return ResponseEntity.ok("Reservation cancelled successfully");
            } else {
                return ResponseEntity.badRequest().body("Failed to cancel reservation");
            }
        }
        catch ( IllegalArgumentException e ) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        catch ( Exception e ) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}