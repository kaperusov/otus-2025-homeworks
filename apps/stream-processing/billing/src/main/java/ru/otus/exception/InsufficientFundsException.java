package ru.otus.exception;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;

    public InsufficientFundsException(UUID userId, BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient funds for user id: %s. Current balance: %s, required: %s",
                userId, currentBalance, requiredAmount));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }
}