package com.bakery.bakeryplatform.controller.order;


import com.bakery.bakeryplatform.dto.order.NoteRequest;
import com.bakery.bakeryplatform.dto.order.PlaceOrderRequest;
import com.bakery.bakeryplatform.dto.order.StatusRequest;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.order.OrderStatus;
import com.bakery.bakeryplatform.service.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order saved = orderService.placeOrder(request.userId(), request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public List<Order> getAll(@RequestParam(required = false) OrderStatus status) {
        return orderService.getAll(status);
    }

    @GetMapping("/user/{userId}")
    public List<Order> getByUser(@PathVariable Long userId) {
        return orderService.getByUser(userId);
    }

    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) {
        return orderService.getById(id);
    }

    @PutMapping("/{id}/status")
    public Order updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @PutMapping("/{id}")
    public Order editNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        return orderService.editNote(id, request.note());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}