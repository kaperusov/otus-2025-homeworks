package ru.otus.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SagaStepResult {
    private boolean success;
    private String message;
    private UUID transactionId; // ID для компенсирующего действия
    private Object data;
}
