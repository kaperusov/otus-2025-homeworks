package ru.otus.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
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
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    @Value("${ru.otus.notification.service.baseUrl:http://localhost:8081}")
    String notificationServiceBaseUrl;

    private static final String SUCCESS_MESSAGE = "Withdrawal successful";
    private static final String BAD_MESSAGE = "Withdrawal failed";

    private final RestTemplate restTemplate;

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        Optional<Account> founded = accountRepository.findByUserId(request.getUserId());
        if (founded.isPresent()) {
            return mapToAccountResponse(founded.get());
        }

        Account account = Account.builder()
                .userId(request.getUserId())
                .email(request.getEmail())
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
            InsufficientFundsException e = new InsufficientFundsException(
                    request.getUserId(),
                    account.getBalance(),
                    request.getAmount());

            sendNotification(account.getEmail(), BAD_MESSAGE, e.getMessage());
            throw e;
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        account = accountRepository.save(account);

        TransactionResponse withdrawalSuccessful = TransactionResponse.builder()
                .success(true)
                .message(SUCCESS_MESSAGE)
                .newBalance(account.getBalance())
                .build();

        DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        sendNotification(
                account.getEmail(),
                SUCCESS_MESSAGE,
                """
                Your order No_%s has been successfully paid for $%s.
                Date: %s
                """.formatted(
                        request.getOrderNumber(),
                        decimalFormat.format(request.getAmount()),
                        LocalDateTime.now().format(dateFormatter))
        );

        return withdrawalSuccessful;
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
                .email(account.getEmail())
                .balance(account.getBalance())
                .build();
    }

    private void sendNotification(String email, String subject, String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("subject", subject);
            body.put("message", message);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> stringResponseEntity = restTemplate.postForEntity(this.notificationServiceBaseUrl + "/notifications",
                    requestEntity,
                    String.class);
            log.debug( "stringResponseEntity: {}", stringResponseEntity );
        }
        catch (Exception e) {
            log.error( e.getMessage(), e );
        }
    }
}