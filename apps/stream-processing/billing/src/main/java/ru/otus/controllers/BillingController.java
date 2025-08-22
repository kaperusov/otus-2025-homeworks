package ru.otus.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.dto.AccountCreateRequest;
import ru.otus.dto.AccountResponse;
import ru.otus.dto.TransactionRequest;
import ru.otus.dto.TransactionResponse;
import ru.otus.exception.AccountNotFoundException;
import ru.otus.exception.InsufficientFundsException;
import ru.otus.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService billingService;

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        try {
            AccountResponse account = billingService.createAccount(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (Exception e ) {
            return buildErrorResponseEntity(e);
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<Object> deposit(@Valid @RequestBody TransactionRequest request) {
        try {
            return ResponseEntity.ok(billingService.deposit(request));
        } catch (Exception e ) {
            return buildErrorResponseEntity(e);
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Object> withdraw(@Valid @RequestBody TransactionRequest request) {
        try {
            return ResponseEntity.ok(billingService.withdraw(request));
        } catch (Exception e ) {
            return buildErrorResponseEntity(e);
        }
    }

    @GetMapping("/accounts/{userId}")
    public ResponseEntity<Object> getAccount(@PathVariable("userId") UUID userId) {
        try {
            return ResponseEntity.ok(billingService.getAccount(userId));
        } catch (Exception e ) {
            return buildErrorResponseEntity(e);
        }
    }

    protected ResponseEntity<Object> buildErrorResponseEntity(Throwable throwable ) {
        log.error( throwable.getMessage(), throwable );
        return switch (throwable) {
            case ResponseStatusException statusException ->
                    ResponseEntity.status(statusException.getStatusCode()).build();
            case IllegalArgumentException illegalArgumentException -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(
                            false,
                            illegalArgumentException.getMessage(),
                            new BigDecimal(0)));
            case AccountNotFoundException accountNotFoundException -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new TransactionResponse(
                            false,
                            accountNotFoundException.getMessage(),
                            new BigDecimal(0)));
            case InsufficientFundsException insufficientFundsException -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new TransactionResponse(
                            false,
                            insufficientFundsException.getMessage(),
                            insufficientFundsException.getCurrentBalance()));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        };
    }
}