package ru.otus.controllers;

import ru.otus.dto.AccountCreateRequest;
import ru.otus.dto.AccountResponse;
import ru.otus.dto.TransactionRequest;
import ru.otus.dto.TransactionResponse;
import ru.otus.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {
    private final BillingService billingService;

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        return billingService.createAccount(request);
    }

    @PostMapping("/deposit")
    public TransactionResponse deposit(@Valid @RequestBody TransactionRequest request) {
        return billingService.deposit(request);
    }

    @PostMapping("/withdraw")
    public TransactionResponse withdraw(@Valid @RequestBody TransactionRequest request) {
        return billingService.withdraw(request);
    }

    @GetMapping("/accounts/{userId}")
    public AccountResponse getAccount(@PathVariable Long userId) {
        return billingService.getAccount(userId);
    }
}