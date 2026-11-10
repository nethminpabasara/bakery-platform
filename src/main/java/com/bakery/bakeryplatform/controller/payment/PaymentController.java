package com.bakery.bakeryplatform.controller.payment;

import com.bakery.bakeryplatform.dto.payment.PaymentRequest;
import com.bakery.bakeryplatform.dto.payment.PaymentStatusRequest;
import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.service.payment.PaymentConfirmationService;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import com.bakery.bakeryplatform.service.user.SiteIdentity;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final SiteIdentity access;
    private final PaymentConfirmationService confirmation;

    public PaymentController(PaymentService paymentService, SiteIdentity access,
                             PaymentConfirmationService confirmation) {
        this.paymentService = paymentService;
        this.access = access;
        this.confirmation = confirmation;
    }

    @PostMapping
    public ResponseEntity<Payment> create(@Valid @RequestBody PaymentRequest request, HttpSession session) {
        Long customerId = access.requireCustomerId(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request, customerId));
    }

    @GetMapping
    public List<Payment> getAll(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.getAll(status);
    }

    @GetMapping("/user/{userId}")
    public List<Payment> getByUser(@PathVariable Long userId, HttpSession session) {
        access.requirePaymentUser(userId, session);
        return paymentService.getByUser(userId);
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> getByOrder(@PathVariable Long orderId, HttpSession session) {
        access.requirePaymentOrder(orderId, session);
        return paymentService.getByOrder(orderId);
    }

    @GetMapping("/{id}")
    public Payment getById(@PathVariable Long id, HttpSession session) {
        Payment payment = paymentService.getById(id);
        access.requirePaymentOrder(payment.getOrderId(), session);
        return payment;
    }

    @GetMapping("/{id}/confirmation")
    public ResponseEntity<PaymentConfirmationService.Confirmation> confirmation(@PathVariable Long id, HttpSession session) {
        Payment payment = paymentService.getById(id);
        access.requirePaymentOrder(payment.getOrderId(), session);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(confirmation.details(payment));
    }

    @PutMapping("/{id}/status")
    public Payment updateStatus(@PathVariable Long id, @Valid @RequestBody PaymentStatusRequest request) {
        return paymentService.updateStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        paymentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
