package com.bakery.bakeryplatform.controller.billing;

import com.bakery.bakeryplatform.model.billing.RefundRequest;
import com.bakery.bakeryplatform.service.billing.RefundService;

import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/refund")
public class RefundController {
    private final RefundService refundService;
    private final OrderRepository orderRepository;

    public RefundController(RefundService refundService, OrderRepository orderRepository) {
        this.refundService = refundService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<RefundRequest> applyForRefund(
            @Valid @RequestBody RefundRequest request, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer login required.");
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id " + request.getOrderId()));
        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only request a refund for your own orders.");
        }

        RefundRequest savedRequest = refundService.submitRefundRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequest);
    }

    @PutMapping("/{id}")
    public RefundRequest processRefund(@PathVariable Long id, @RequestParam String status) {
        return refundService.updateRefundStatus(id, status);
    }

    @GetMapping
    public List<RefundRequest> viewAllRefundRequest() {
        return refundService.getAllRefundRequest();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelRefundRequest(@PathVariable Long id) {
        refundService.withdrawRefundRequest(id);
        return ResponseEntity.noContent().build();
    }
}
