package com.bakery.bakeryplatform.repository.order;

import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.order.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByStatus(OrderStatus status);
}