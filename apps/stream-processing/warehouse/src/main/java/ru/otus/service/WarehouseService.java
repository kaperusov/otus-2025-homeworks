package ru.otus.service;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.otus.dto.ReserveItemRequest;
import ru.otus.dto.ReserveResult;
import ru.otus.models.Reservation;
import ru.otus.models.Stock;
import ru.otus.repository.ReservationRepository;
import ru.otus.repository.StockRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService {

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public ReserveResult reserveItems(ReserveItemRequest request) {
        // Проверяем, не была ли уже создана резервация для этого заказа
        if (reservationRepository.existsByOrderId(request.getOrderId())) {
            return new ReserveResult(false, "Reservation already exists for this order", null);
        }

        try {
            log.info("Starting reservation for order {}", request.getOrderId());

            List<Stock> stockList = new ArrayList<>(request.getItems().size());
            // Проверяем доступность всех товаров перед резервированием
            for (var item : request.getItems()) {
                Stock stock = getOrCreateStock(item.getProductId());
                stockList.add( stock );
//                Stock stock = stockRepository.findByProductIdWithLock(item.getProductId())
//                        .orElseThrow(() -> new RuntimeException(
//                                "Product " + item.getProductId() + " not found in warehouse"));

                if (stock.getAvailableQuantity() < item.getQuantity()) {
                    throw new RuntimeException(
                            "Not enough stock for product " + item.getProductId() +
                            ". Available: " + stock.getAvailableQuantity() +
                            ", Requested: " + item.getQuantity());
                }
            }

            // Создаем запись о резервации
            Reservation reservation = new Reservation();
            reservation.setOrderId(request.getOrderId());
            reservation.setCreatedAt(LocalDateTime.now());
            reservation.setStatus("RESERVED");
            reservation = reservationRepository.save(reservation);

            // Резервируем товары
            for (var stock : stockList ) {
                stock.setReservedQuantity(stock.getReservedQuantity() + 1 );
                stock.setReservation( reservation );
                stockRepository.save(stock);

                log.debug("Reserved {} units of product {} for order {}. Available quantity: {}",
                        stock.getReservedQuantity(), stock.getProductId(), request.getOrderId(), stock.getAvailableQuantity());
            }

            log.info("Reservation completed successfully for order {}. Reservation ID: {}",
                    request.getOrderId(), reservation.getId());

            return new ReserveResult(true, "Reservation successful", reservation.getId());

        } catch (Exception e) {
            log.error("Error during reservation for order {}: {}",
                    request.getOrderId(), e.getMessage());
            return new ReserveResult(false, e.getMessage(), null);
        }
    }

    private Stock getOrCreateStock(UUID productId) {

        Optional<Stock> opt = stockRepository.findByProductIdWithLock(productId);
        if ( opt.isPresent()) {
            return opt.get();
        }
        else {
            Stock stock = new Stock();
            stock.setProductId( productId );
            stock.setTotalQuantity( 10 );
            stock.setReservedQuantity( 0 );

            return stockRepository.save( stock );
        }
    }

    // Метод для отмены резервации
    @Transactional
    public boolean cancelReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findByIdAndAndStatus(reservationId, "RESERVED")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Reservation " + reservationId + " not found in warehouse"));

        reservation.setStatus("CANCELED");
        for( Stock stock : stockRepository.findAllByReservationId( reservationId )) {
            stock.setReservedQuantity(stock.getReservedQuantity() - 1);
            stockRepository.save( stock );
            log.info("Canceled reservation: {}, stock available quantity: {}", reservationId, stock.getAvailableQuantity() );
        }

        Reservation canceledReservation = reservationRepository.save(reservation);

        return canceledReservation.getStatus().equals("CANCELED");
    }
}