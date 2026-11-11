package com.bakery.bakeryplatform.service.payment;

import com.bakery.bakeryplatform.service.billing.InvoiceService;
import com.bakery.bakeryplatform.model.billing.Invoice;
import com.bakery.bakeryplatform.service.billing.InvoiceEmailService;
import com.bakery.bakeryplatform.dto.payment.PaymentRequest;
import com.bakery.bakeryplatform.exception.BusinessRuleException;
import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.payment.CardPayment;
import com.bakery.bakeryplatform.model.payment.CashOnDelivery;
import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.payment.PaymentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final InvoiceService invoiceService;
    private final InvoiceEmailService invoiceEmailService;

    public PaymentService(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            InvoiceService invoiceService,
            InvoiceEmailService invoiceEmailService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.invoiceService = invoiceService;
        this.invoiceEmailService = invoiceEmailService;
    }

    // Create record  payment for an order
    @Transactional
    public Payment create(PaymentRequest request, Long customerId) {
        // Lock the order while checking and recording payment, including double clicks.
        Order order = orderRepository.findByIdForPayment(request.orderId()).orElse(null);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with id " + request.orderId());
        }
        if (!customerId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only pay for your own orders.");
        }
        if (order.getTotalAmount() == null || order.getTotalAmount().signum() <= 0) {
            throw new BusinessRuleException("The order must have a positive total before payment.");
        }
        if (paymentRepository.existsByOrderIdAndStatusIn(order.getId(),
                List.of(PaymentStatus.PAID, PaymentStatus.PENDING))) {
            throw new BusinessRuleException("This order already has a paid or pending payment.");
        }

        Payment payment;
        if (request.paymentType().equals("CARD")) {
            payment = buildCardPayment(order, request);
        } else {
            payment = new CashOnDelivery(order.getId(), order.getTotalAmount());
        }

        payment.setFee(payment.calculateFee());              // polymo: each type has its own fee
        payment.setReceiptNote(payment.processPayment());    // polymo: each type has its own message
        Payment savedPayment = paymentRepository.save(payment);
        if (savedPayment.getStatus() == PaymentStatus.PAID) {
            Invoice invoice = invoiceService.generateInvoice(savedPayment, order.getUserId());
            invoiceEmailService.queue(invoice.getId());
        }
        return savedPayment;
    }

    // Read
    public List<Payment> getAll(PaymentStatus status) {
        if (status == null) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByStatus(status);
    }

    public List<Payment> getByOrder(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    // Payment history of one customer = payments of that customer's orders
    public List<Payment> getByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserId(userId);
        List<Long> orderIds = new ArrayList<>();
        for (Order order : orders) {
            orderIds.add(order.getId());
        }
        if (orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        return paymentRepository.findByOrderIdIn(orderIds);
    }

    public Payment getById(Long id) {
        return findPayment(id);
    }

    // Update: PAID -> REFUNDED
    @Transactional
    public Payment updateStatus(Long id, PaymentStatus newStatus) {
        Long orderId = paymentRepository.findOrderIdForPayment(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found."));
        Order order = orderRepository.findByIdForPayment(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("The payment's order was not found."));
        // Reload after taking the same lock used by create(), so concurrent updates agree.
        Payment payment = findPayment(id);
        if (!payment.getStatus().canMoveTo(newStatus)) {
            throw new BusinessRuleException(
                    "A payment cannot move from " + payment.getStatus() + " to " + newStatus + ".");
        }
        payment.setStatus(newStatus);
        Payment saved = paymentRepository.save(payment);
        if (newStatus == PaymentStatus.PAID) {
            Invoice invoice = invoiceService.generateInvoice(saved, order.getUserId());
            invoiceEmailService.queue(invoice.getId());
        }
        return saved;
    }

    // Delete: only refunded records can be removed
    public void delete(Long id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() != PaymentStatus.REFUNDED) {
            throw new BusinessRuleException("Only refunded payments can be deleted.");
        }
        paymentRepository.delete(payment);
    }

    private Payment buildCardPayment(Order order, PaymentRequest request) {
        String holder = request.cardHolderName();
        String number = request.cardNumber();
        if (holder == null || holder.trim().isEmpty() || number == null || !number.matches("\\d{16}")) {
            throw new BusinessRuleException("Card holder name and a 16-digit card number are required for card payments.");
        }
        String last4 = number.substring(number.length() - 4);   // keep only last 4 digits
        return new CardPayment(order.getId(), order.getTotalAmount(), holder.trim(), last4);
    }

    private Payment findPayment(Long id) {
        Payment payment = paymentRepository.findById(id).orElse(null);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found with id " + id);
        }
        return payment;
    }
}
