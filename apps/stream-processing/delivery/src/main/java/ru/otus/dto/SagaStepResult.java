package ru.otus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.UUID;

@Data
@AllArgsConstructor
public class SagaStepResult {
    private boolean success;
    private String message;
    private UUID transactionId; // ID для компенсирующего действия
    private Object data;
}
