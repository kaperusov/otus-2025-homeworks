package ru.otus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import ru.otus.controlles.models.Order;
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

    Random random = new Random();

    @Transactional
    public Order createOrder(Order request) {

        try {
            if ( withdraw(request.getUserId(), request.getPrice())) {
                Order order = Order.builder()
                        .name(request.getName())
                        .price(request.getPrice())
                        .userId(request.getUserId())
                        .description(request.getDescription())
                        .number(new BigDecimal(random.nextLong()))
                        .build();

                return orderRepository.save(order);
            }
            else {
                return null;
            }
        } catch (Exception e ) {
            rollbackMoney(request.getUserId(), request.getPrice());
            throw e;
        }
    }


    private boolean withdraw(UUID userId, BigDecimal price) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("amount", price);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                this.billingServiceBaseUrl + "/withdraw",
                requestEntity,
                String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK ) {
            log.debug("withdraw: {}", responseEntity);
            return true;
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
