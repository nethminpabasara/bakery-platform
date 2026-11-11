package com.bakery.bakeryplatform.repository.user;

import com.bakery.bakeryplatform.model.user.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findFirstByOrderId(Long orderId);

    void deleteByCustomerId(Long customerId);
}
