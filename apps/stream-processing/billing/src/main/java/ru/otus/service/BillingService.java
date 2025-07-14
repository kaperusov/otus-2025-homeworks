package ru.otus.service;

import ru.otus.dto.AccountCreateRequest;
import ru.otus.dto.AccountResponse;
import ru.otus.dto.TransactionRequest;
import ru.otus.dto.TransactionResponse;
import ru.otus.exception.AccountNotFoundException;
import ru.otus.exception.InsufficientFundsException;
import ru.otus.models.Account;
import ru.otus.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        Optional<Account> founded = accountRepository.findByUserId(request.getUserId());
        if (founded.isPresent()) {
            return mapToAccountResponse(founded.get());
        }

        Account account = Account.builder()
                .userId(request.getUserId())
                .balance(BigDecimal.ZERO)
                .build();

        account = accountRepository.save(account);
        return mapToAccountResponse(account);
    }

    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        Account account = accountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new AccountNotFoundException(request.getUserId()));

        account.setBalance(account.getBalance().add(request.getAmount()));
        account = accountRepository.save(account);

        return TransactionResponse.builder()
                .success(true)
                .message("Deposit successful")
                .newBalance(account.getBalance())
                .build();
    }

    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {
        Account account = accountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new AccountNotFoundException(request.getUserId()));

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    request.getUserId(),
                    account.getBalance(),
                    request.getAmount());
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account = accountRepository.save(account);

        return TransactionResponse.builder()
                .success(true)
                .message("Withdrawal successful")
                .newBalance(account.getBalance())
                .build();
    }

    public AccountResponse getAccount(UUID userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException(userId));
        return mapToAccountResponse(account);
    }

    private AccountResponse mapToAccountResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .build();
    }
}