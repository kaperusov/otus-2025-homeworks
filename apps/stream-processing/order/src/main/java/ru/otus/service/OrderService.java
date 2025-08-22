package ru.otus.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.otus.models.Order;
import ru.otus.models.STATUS;
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

    private static final String BILLING_SERVICE_BASEURL = System.getenv("BILLING_SERVICE_BASEURL");
    private static final String WAREHOUSE_SERVICE_BASEURL = System.getenv("WAREHOUSE_SERVICE_BASEURL");
    private static final String DELIVER_SERVICE_BASEURL = System.getenv("DELIVER_SERVICE_BASEURL");

    private static final String USER_ID = "userId";
    private static final String AMOUNT = "amount";
    private static final String ORDER_NUMBER = "orderNumber";

    private static final String ORDER_ID = "orderId";
    private static final String PRODUCT_ID = "productId";
    private static final String PRODUCT_QUANTITY = "quantity";



    private final RestTemplate restTemplate;

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
//                .orderId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(request.getQuantity())
                .build();

        Order newOrder = orderRepository.save(order);
        if ( withdraw(request.getUserId(), request.getPrice(), newOrder.getNumber())) {
            return updateStatus( newOrder.getId(), STATUS.PAID );
        } else {
            return updateStatus( newOrder.getId(), STATUS.CANCELLED );
        }
    }


    private boolean withdraw(UUID userId, BigDecimal price, BigDecimal orderNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put(USER_ID, userId);
            body.put(AMOUNT, price);
            body.put(ORDER_NUMBER, orderNumber);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    makeUrl( BILLING_SERVICE_BASEURL, "/withdraw" ),
                    requestEntity,
                    String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("withdraw: {}", responseEntity);
                return true;
            }
        } catch ( Exception e ) {
            log.error("Withdraw FAILED: {}", e.getMessage(), e);
        }
        return false;
    }

    public Order reserve(Order order) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put(ORDER_ID, order.getId());
            body.put(PRODUCT_ID, order.getProductId());
            body.put(PRODUCT_QUANTITY, order.getQuantity());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    makeUrl( WAREHOUSE_SERVICE_BASEURL, "/reserve" ),
                    requestEntity,
                    String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("reservation: {}", responseEntity);
                return updateStatus( order.getId(), STATUS.PROCESSING );
            }
        } catch ( Exception e ) {
            log.error("Reservation FAILED: {}", e.getMessage(), e);
        }
        return order;
    }


    public Order updateStatus(UUID id, STATUS status ) {
        Order order = orderRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException( HttpStatus.NOT_FOUND ));
        order.setStatus( status );
        Order updated =  orderRepository.save( order );
        log.debug("Updated order status: '{}' (ID={})", status, id);
        return updated;
    }

    public Order deliver(Order order) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put(USER_ID, order.getUserId());
            body.put(ORDER_NUMBER, order.getNumber());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                    makeUrl( DELIVER_SERVICE_BASEURL, "/deliver" ),
                    requestEntity,
                    String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                log.debug("deliver: {}", responseEntity);
                return updateStatus( order.getId(), STATUS.SHIPPED );
            }
        } catch ( Exception e ) {
            log.error("Deliver FAILED: {}", e.getMessage(), e);
        }
        return order;
    }


    public Order rollbackMoney(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put(USER_ID, order.getUserId());
        body.put(AMOUNT, order.getPrice());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                makeUrl( BILLING_SERVICE_BASEURL,"/deposit" ),
                requestEntity,
                String.class);

        log.info("rollbackMoney: {}", responseEntity);
        return updateStatus( order.getId(), STATUS.CANCELLED );
    }


    public Order rollbackWarehouseProcessing(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put(ORDER_NUMBER, order.getNumber());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                makeUrl( WAREHOUSE_SERVICE_BASEURL,"/rollback" ),
                requestEntity,
                String.class);

        log.info("Rollback warehouse processing: {}", responseEntity);
        return updateStatus( order.getId(), STATUS.PAID );
    }


    public Order rollbackDelivering(Order order) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put(ORDER_NUMBER, order.getNumber());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(
                makeUrl( DELIVER_SERVICE_BASEURL,"/rollback" ),
                requestEntity,
                String.class);

        log.info("Rollback delivering: {}", responseEntity);
        return updateStatus( order.getId(), STATUS.PROCESSING );
    }


    private static String makeUrl(@NonNull String baseUrl, @NonNull String endpoint) {
        if (!StringUtils.hasText(baseUrl)) {
            baseUrl = "http://arch.homework/api/v1";
        }
        String url = baseUrl + endpoint;
        log.debug( "Make URL for request to service: {}", url );
        return url;
    }
}
