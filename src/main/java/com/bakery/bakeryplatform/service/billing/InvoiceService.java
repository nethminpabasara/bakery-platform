package com.bakery.bakeryplatform.service.billing;

import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.model.billing.PaymentInvoice;
import com.bakery.bakeryplatform.repository.billing.InvoiceRepository;

import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import com.bakery.bakeryplatform.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class InvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository, UserRepository userRepository,
                          OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Invoice generateInvoice(Payment payment, Long userId) {
        if (payment.getStatus() != PaymentStatus.PAID && payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new BusinessRuleException("An invoice can only be generated after payment.");
        }
        return invoiceRepository.findByPaymentId(payment.getId()).orElseGet(() -> {
            PaymentInvoice invoice = new PaymentInvoice();
            invoice.setPaymentId(payment.getId());
            invoice.setOrderId(payment.getOrderId());
            invoice.setUserId(userId);
            invoice.setPaymentType(payment.getPaymentType());
            invoice.setAmount(payment.getTotalCharged());
            invoice.setSubtotal(payment.getAmount());
            invoice.setPaymentFee(payment.getFee());
            userRepository.findById(userId).ifPresent(customer -> {
                invoice.setCustomerName(customer.getUsername());
                invoice.setCustomerEmail(customer.getEmail());
            });
            orderRepository.findById(payment.getOrderId()).ifPresent(order ->
                    invoice.setItemsSummary(order.getItemsSummary()));
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setInvoiceNumber("INV-PAY-" + invoice.getInvoiceDate().getYear()
                    + "-" + String.format("%06d", payment.getId()));
            return invoiceRepository.save(invoice);
        });
    }

    public Invoice getInvoiceByIdAndUserId(Long id, Long userId) {
        return invoiceRepository.findById(id)
                .filter(invoice -> userId.equals(invoice.getUserId()))
                .orElse(null);
    }

    public List<Invoice> getInvoicesByUserId(Long userId) {
        return invoiceRepository.findByUserIdOrderByInvoiceDateDesc(userId);
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc();
    }

}
