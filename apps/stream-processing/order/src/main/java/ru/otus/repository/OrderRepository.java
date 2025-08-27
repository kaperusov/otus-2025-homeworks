package ru.otus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.otus.models.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository  extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);

//    @Query("""
//        SELECT CASE
//            WHEN COUNT(o) > 0 THEN TRUE
//            ELSE FALSE
//            END
//        FROM Order o
//        JOIN o.items i
//        WHERE o.userId = :userId
//        AND o.createdAt >= :sinceTime
//        AND o.status NOT IN (ru.otus.models.OrderStatus.CONFIRMED, ru.otus.models.OrderStatus.FAILED)
//        AND i.name IN :itemNames
//        GROUP BY o.id
//        HAVING COUNT(i) >= :minMatchingItems
//    """)
//    Boolean existsSimilarOrder(
//            @Param("userId") UUID userId,
//            @Param("itemNames") List<String> itemNames,
//            @Param("sinceTime") LocalDateTime sinceTime,
//            @Param("minMatchingItems") long minMatchingItems
//    );

    @Query("""
        SELECT o
        FROM Order o
        JOIN o.items i
        WHERE o.userId = :userId
        AND o.createdAt >= :sinceTime
        AND o.status != ru.otus.models.OrderStatus.FAILED
        AND i.name IN :itemNames
    """)
    Optional<Order> existsSimilarOrder(
            @Param("userId") UUID userId,
            @Param("itemNames") List<String> itemNames,
            @Param("sinceTime") LocalDateTime sinceTime,
            @Param("minMatchingItems") long minMatchingItems
    );

//    boolean existsByUserIdAndItemsAndDeliveryInfo(UUID userId, List<OrderRequest.OrderItem> items, OrderRequest.DeliveryInfo deliveryInfo);
}
