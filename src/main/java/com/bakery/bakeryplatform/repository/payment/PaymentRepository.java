package com.bakery.bakeryplatform.repository.payment;

import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByOrderIdIn(List<Long> orderIds);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<PaymentStatus> statuses);

    @Query("select p.orderId from Payment p where p.id = :id")
    Optional<Long> findOrderIdForPayment(@Param("id") Long id);
}
