package ru.otus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@AllArgsConstructor
@Data
public class ReserveResult {
    private boolean success;
    private String message;
    private UUID reservationId;
}