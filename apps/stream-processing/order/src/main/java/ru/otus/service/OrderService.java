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
import ru.otus.models.OrderRequest;
import ru.otus.models.OrderStatus;
import ru.otus.models.SagaStepResult;
import ru.otus.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    // Bean injection
    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;
    // ---

    private static final String BILLING_SERVICE_BASEURL = System.getenv("BILLING_SERVICE_BASEURL");
    private static final String WAREHOUSE_SERVICE_BASEURL = System.getenv("WAREHOUSE_SERVICE_BASEURL");
    private static final String DELIVERY_SERVICE_BASEURL = System.getenv("DELIVERY_SERVICE_BASEURL");

    // Map fields
    private static final String USER_ID = "userId";
    private static final String AMOUNT = "amount";
    private static final String ORDER_ID = "orderId";
    private static final String ORDER_ITEMS = "items";

    private final Random random = new Random();

    private String generateOrderNumber() {
        long min = 1_000_000_000L;
        long max = 999_999_999_999L;
        BigDecimal number = new BigDecimal(random.nextLong(min, max));
        String formatted = String.format("%012d", number.longValue());
        return formatted.substring(0, 4) + "-" +
               formatted.substring(4, 7) + "-" +
               formatted.substring(7, 10) + " " +
               formatted.substring(10);
    }

    private String generateDescription(List<OrderRequest.OrderItem> items) {
        StringBuilder description = new StringBuilder( "Products: " );
        for (OrderRequest.OrderItem item : items) {
            description.append( item.getName());
            description.append( ", " );
        }
        return description.substring(0, description.length() - 2 );
    }

    private BigDecimal calcAmount(List<OrderRequest.OrderItem> items) {
        BigDecimal amount = BigDecimal.ZERO;
        for (OrderRequest.OrderItem item : items) {
            BigDecimal q = BigDecimal.valueOf( item.getQuantity());
            amount = amount.add( item.getPrice().multiply( q ));
        }
        return amount;
    }

    @Transactional
    public Order createOrder(OrderRequest request) {
        if ( request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items can't by empty.");
        }

        Order order = orderRepository.save(Order.builder()
                .price(calcAmount(request.getItems()))
                .number(generateOrderNumber())
                .status(OrderStatus.NEW)
                .description(generateDescription(request.getItems()))
                .userId(request.getUserId())
                .build());

        UUID orderId = order.getId();

        try {
            // Шаг 1: Выполнение платежа
            SagaStepResult paymentResult = processPayment(request.getUserId(), orderId, request.getItems());
            if (!paymentResult.isSuccess()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment failed: " + paymentResult.getMessage());
            }

            // Шаг 2: Резервирование товара
            SagaStepResult warehouseResult = reserveItems(orderId, request.getItems());
            if (!warehouseResult.isSuccess()) {
                cancelPayment(orderId, paymentResult.getTransactionId());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Warehouse reservation failed: " + warehouseResult.getMessage());
            }

            // Шаг 3: Резервирование доставки
            SagaStepResult deliveryResult = reserveDelivery(orderId, request.getDeliveryInfo());
            if (!deliveryResult.isSuccess()) {
                cancelPayment(orderId, paymentResult.getTransactionId());
                cancelReservation(orderId, warehouseResult.getTransactionId());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Delivery reservation failed: " + deliveryResult.getMessage());
            }

            // Все шаги успешны
            return updateOrderStatus(orderId, OrderStatus.CONFIRMED, null);
        }
        catch (Exception e) {
            updateOrderStatus(orderId, OrderStatus.FAILED, e.getMessage());
            throw e;
        }
    }

    @NonNull
    private SagaStepResult processPayment(UUID userId, UUID orderId, List<OrderRequest.OrderItem> items) {
        try {
            BigDecimal amount = calcAmount( items );
            ResponseEntity<SagaStepResult> responseEntity = restTemplate.postForEntity(
                    makeUrl( BILLING_SERVICE_BASEURL, "/withdraw" ),
                    Map.of(
                            USER_ID, userId,
                            ORDER_ID, orderId,
                            AMOUNT, amount
                    ),
                    SagaStepResult.class);

            return Objects.requireNonNullElseGet(responseEntity.getBody(),
                    () -> new SagaStepResult(false, "Billing service result body is empty", null, null));
        }
        catch ( Exception e ) {
            log.error( e.getMessage(), e );
            return new SagaStepResult(false, "billing service error: " + e.getMessage(), null, null);
        }
    }

    @NonNull
    private SagaStepResult reserveItems(UUID orderId, List<OrderRequest.OrderItem> items) {
        try {
            ResponseEntity<SagaStepResult> responseEntity = restTemplate.postForEntity(
                    makeUrl( WAREHOUSE_SERVICE_BASEURL, "/reserve" ),
                    Map.of(
                            ORDER_ID, orderId,
                            ORDER_ITEMS, items
                    ),
                    SagaStepResult.class);

            return Objects.requireNonNullElseGet(responseEntity.getBody(),
                    () -> new SagaStepResult(false, "Warehouse service result body is empty", null, null));
        }
        catch ( Exception e ) {
            log.error("Reservation FAILED: {}", e.getMessage(), e);
            return new SagaStepResult(false, "Warehouse service error: " + e.getMessage(), null, null);
        }
    }

    @NonNull
    private SagaStepResult reserveDelivery(UUID orderId, OrderRequest.DeliveryInfo deliveryInfo) {
        try {
            ResponseEntity<SagaStepResult> responseEntity = restTemplate.postForEntity(
                    makeUrl(DELIVERY_SERVICE_BASEURL, "/reserve" ),
                    Map.of(
                            ORDER_ID, orderId,
                            "address", deliveryInfo.getAddress(),
                            "preferredTimeSlot", deliveryInfo.getPreferredTimeSlot()
                    ),
                    SagaStepResult.class);

            return Objects.requireNonNullElseGet(responseEntity.getBody(),
                    () -> new SagaStepResult(false, "Delivery service result body is empty", null, null));
        }
        catch (Exception e) {
            return new SagaStepResult(false, "Delivery service error: " + e.getMessage(), null, null);
        }
    }


    private void cancelPayment(UUID orderId, UUID transactionId) {
        try {
            restTemplate.postForEntity(
                    makeUrl( BILLING_SERVICE_BASEURL,"/cancel/" + transactionId ),
                    null,
                    Void.class);
        } catch (Exception e) {
            log.error("Failed to cancel payment for order {}: {}", orderId, e.getMessage());
        }
    }


    private void cancelReservation(UUID orderId, UUID transactionId) {
        try {
            restTemplate.postForEntity(
                    makeUrl(WAREHOUSE_SERVICE_BASEURL, "/reserve/cancel/" + transactionId),
                    null,
                    Void.class);
        }
        catch (Exception e ) {
            log.error("Failed to cancel reservation for order {}: {}", orderId, e.getMessage());
        }
    }


    private Order updateOrderStatus(UUID orderId, OrderStatus status, String errorMessage) {
        // Загружаем свежую версию заказа
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(status);
        order.setErrorMessage(errorMessage);

        return orderRepository.save(order);
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
