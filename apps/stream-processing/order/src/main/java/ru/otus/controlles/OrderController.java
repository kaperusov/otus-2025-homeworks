package ru.otus.controlles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import ru.otus.models.Order;
import ru.otus.dto.OrderRequest;
import ru.otus.service.IdempotencyService;
import ru.otus.service.OrderService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> createOrder(@Valid @RequestBody OrderRequest request) {

        // Проверяем идемпотентность по ключу (краткосрочная)
        if (request.getIdempotencyKey() != null) {
            Object cachedResult = idempotencyService.getCachedResult(request.getIdempotencyKey());
            if (cachedResult != null) {
                log.info("Returning cached result for idempotency key: {}", request.getIdempotencyKey());
                return ResponseEntity.status(HttpStatus.OK).body(cachedResult);
            }

            if (idempotencyService.isDuplicateRequest(request.getIdempotencyKey())) {
                log.warn("Duplicate request with idempotency key: {}", request.getIdempotencyKey());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Duplicate request. Use unique idempotency key.");
            }
        }

        // Проверяем дубликаты по БД (долгосрочная)
        Optional<Order> duplicateOrder = orderService.findDuplicateOrder(request);
        if (duplicateOrder.isPresent()) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(duplicateOrder);
        }


        try {
            Order order = orderService.createOrder(request);
            // Сохраняем результат для будущих дубликатов
            if (request.getIdempotencyKey() != null) {
                idempotencyService.storeRequestResult(request.getIdempotencyKey(), order);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        }
        catch (HttpClientErrorException e ) {
            log.error(e.getMessage(), e);
            return ResponseEntity
                    .status(e.getStatusCode())
                    .body(e.getResponseBodyAsString());
        }
        catch (Exception e ) {
            log.error(e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

}
