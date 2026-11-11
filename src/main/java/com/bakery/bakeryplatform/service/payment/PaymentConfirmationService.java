package com.bakery.bakeryplatform.service.payment;

import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.model.billing.Invoice.EmailStatus;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PaymentConfirmationService {
    private final InvoiceRepository invoices;
    private final OrderRepository orders;

    public PaymentConfirmationService(InvoiceRepository invoices, OrderRepository orders) {
        this.invoices = invoices;
        this.orders = orders;
    }

    public Confirmation details(Payment payment) {
        Invoice invoice = invoices.findByPaymentId(payment.getId()).orElse(null);
        String summary = invoice != null ? invoice.getItemsSummary()
                : orders.findById(payment.getOrderId()).map(order -> order.getItemsSummary()).orElse("");
        return new Confirmation(payment.getId(), payment.getOrderId(), payment.getStatus(),
                payment.getPaymentType(), payment.getAmount(), payment.getFee(), payment.getTotalCharged(), summary,
                invoice == null ? null : invoice.getId(), invoice == null ? null : invoice.getInvoiceNumber(),
                invoice == null ? null : invoice.getCustomerEmail(),
                invoice == null ? EmailStatus.NOT_REQUESTED : invoice.getEmailStatus(),
                "CARD".equals(payment.getPaymentType()));
    }

    public record Confirmation(Long paymentId, Long orderId, PaymentStatus status, String paymentType,
                               BigDecimal amount, BigDecimal fee, BigDecimal totalCharged, String itemsSummary,
                               Long invoiceId, String invoiceNumber, String customerEmail,
                               EmailStatus emailStatus, boolean demoPayment) { }
}
