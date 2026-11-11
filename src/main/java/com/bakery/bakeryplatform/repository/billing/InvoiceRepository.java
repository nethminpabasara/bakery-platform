package com.bakery.bakeryplatform.repository.billing;

import com.bakery.bakeryplatform.model.billing.Invoice;

import com.bakery.bakeryplatform.model.billing.Invoice.EmailStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUserIdOrderByInvoiceDateDesc(Long userId);

    List<Invoice> findAllByOrderByInvoiceDateDesc();

    Optional<Invoice> findByPaymentId(Long paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id")
    Optional<Invoice> findByIdForEmail(@Param("id") Long id);

    List<Invoice> findTop20ByEmailStatusOrderByIdAsc(EmailStatus status);
}
