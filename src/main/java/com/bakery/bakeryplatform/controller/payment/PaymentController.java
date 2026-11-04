package com.bakery.bakeryplatform.controller.payment;

import com.bakery.bakeryplatform.dto.payment.PaymentRequest;
import com.bakery.bakeryplatform.dto.payment.PaymentStatusRequest;
import com.bakery.bakeryplatform.model.payment.Payment;
import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import com.bakery.bakeryplatform.service.payment.PaymentService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Payment> create(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.create(request));
    }

    @GetMapping
    public List<Payment> getAll(@RequestParam(required = false) PaymentStatus status) {
        return paymentService.getAll(status);
    }

    @GetMapping("/user/{userId}")
    public List<Payment> getByUser(@PathVariable Long userId) {
        return paymentService.getByUser(userId);
    }

    @GetMapping("/order/{orderId}")
    public List<Payment> getByOrder(@PathVariable Long orderId) {
        return paymentService.getByOrder(orderId);
    }

    @GetMapping("/{id}")
    public Payment getById(@PathVariable Long id) {
        return paymentService.getById(id);
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