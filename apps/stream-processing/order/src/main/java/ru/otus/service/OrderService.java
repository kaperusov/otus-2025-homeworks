package ru.otus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.otus.controlles.models.Order;
import ru.otus.controlles.models.STATUS;
import ru.otus.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final RestTemplate restTemplate;

    @Value("${ru.otus.billing.service.baseUrl:http://localhost:8080/api/v1/billing}")
    String billingServiceBaseUrl;

    final OrderRepository orderRepository;

    private final Random random = new Random();

    private BigDecimal getOrderNumber() {
        long min = 200_000_000L;
        long max = 999_999_999L;
        return new BigDecimal(random.nextLong(min, max));
    }


    @Transactional
    public Order createOrder(Order request) {

        Order order = Order.builder()
                .name(request.getName())
                .price(request.getPrice())
                .status(STATUS.NEW)
                .userId(request.getUserId())
                .description(request.getDescription())
                .number(getOrderNumber())
                .build();

        Order newOrder = orderRepository.save(order);
        if ( withdraw(request.getUserId(), request.getPrice(), newOrder.getNumber())) {
            newOrder.setStatus( STATUS.PAID );
        } else {
            newOrder.setStatus( STATUS.CANCELLED );
        }
        return orderRepository.save(newOrder);
    }


    private boolean withdraw(UUID userId, BigDecimal price, BigDecimal orderNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("userId", userId);
            body.put("amount", price);
            body.put("orderNumber", orderNumber);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    this.billingServiceBaseUrl + "/withdraw",
                    requestEntity,
                    String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("withdraw: {}", responseEntity);
                return true;
            }
        } catch ( Exception e ) {
            log.error("Withdraw FAILED: " + e.getMessage(), e);
        }
        return false;
    }


    private void rollbackMoney(UUID userId, BigDecimal price) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", price);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                this.billingServiceBaseUrl + "/deposit",
                requestEntity,
                String.class);

        log.debug("rollbackMoney: {}", responseEntity);
    }
}
