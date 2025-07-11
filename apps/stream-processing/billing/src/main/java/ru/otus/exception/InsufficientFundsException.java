package ru.otus.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(Long userId, BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient funds for user id: %s. Current balance: %s, required: %s",
                userId, currentBalance, requiredAmount));
    }
}