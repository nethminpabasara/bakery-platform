package com.bakery.bakeryplatform.repository.order;

import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.order.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForPayment(@Param("id") Long id);

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);
    java.util.Optional<Order> findByCheckoutToken(String token);
}
