package com.bakery.bakeryplatform.service.payment;

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

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    // Create record  payment for an order
    public Payment create(PaymentRequest request) {
        Order order = orderRepository.findById(request.orderId()).orElse(null);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with id " + request.orderId());
        }
        if (paymentRepository.existsByOrderIdAndStatus(order.getId(), PaymentStatus.PAID)) {
            throw new BusinessRuleException("This order is already paid.");
        }

        Payment payment;
        if (request.paymentType().equals("CARD")) {
            payment = buildCardPayment(order, request);
        } else {
            payment = new CashOnDelivery(order.getId(), order.getTotalAmount());
        }

        payment.setFee(payment.calculateFee());              // polymo: each type has its own fee
        payment.setReceiptNote(payment.processPayment());    // polymo: each type has its own message
        return paymentRepository.save(payment);
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
    public Payment updateStatus(Long id, PaymentStatus newStatus) {
        Payment payment = findPayment(id);
        if (!payment.getStatus().canMoveTo(newStatus)) {
            throw new BusinessRuleException(
                    "A payment cannot move from " + payment.getStatus() + " to " + newStatus + ".");
        }
        payment.setStatus(newStatus);
        return paymentRepository.save(payment);
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
        if (holder == null || holder.trim().isEmpty() || number == null) {
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