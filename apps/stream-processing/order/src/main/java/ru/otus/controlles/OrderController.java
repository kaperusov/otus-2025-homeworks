package ru.otus.controlles;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import ru.otus.models.Order;
import ru.otus.models.STATUS;
import ru.otus.service.OrderService;

@RestController
@RequestMapping("/api/v1/order")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping("{testTransaction}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Object> createOrder(@Valid @RequestBody Order request,
                                              @PathVariable("testTransaction") int testTransaction ) {
        try {
            Order order = orderService.createOrder(request);

            if ( STATUS.PAID.equals( order.getStatus() ) && testTransaction >= 0 )  {
                order = orderService.reserve( order );

                if ( STATUS.PROCESSING.equals( order.getStatus() ) && testTransaction >= 1 )  {
                    order = orderService.deliver( order );

                    if ( STATUS.SHIPPED.equals( order.getStatus()) && testTransaction >= 2 ) {
                        log.info("Finished order status: {}", order.getStatus());
                    }
                    else {
                        order = rollbackTransaction(order, STATUS.SHIPPED);
                    }
                }
                else {
                    order = rollbackTransaction(order, STATUS.PROCESSING);
                }
            }
            else {
                order = rollbackTransaction(order, STATUS.PAID);
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

    private Order rollbackTransaction(Order order, STATUS status) {
        return switch (status) {
            case PAID -> orderService.rollbackMoney(order);
            case PROCESSING -> orderService.rollbackWarehouseProcessing(order);
            case SHIPPED -> orderService.rollbackDelivering(order);

            default ->  orderService.updateStatus(order.getId(), STATUS.FAILED);
        };
    }
}
