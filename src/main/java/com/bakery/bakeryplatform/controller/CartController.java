package com.bakery.bakeryplatform.controller;

import com.bakery.bakeryplatform.dto.CartResponse;
import com.bakery.bakeryplatform.dto.QuantityRequest;
import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    @PostMapping
    public ResponseEntity<CartItem> addToCart(@Valid @RequestBody CartItem item) {
        CartItem saved = cartService.addToCart(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    @GetMapping("/user/{userId}")
    public CartResponse getCart(@PathVariable Long userId,
                                @RequestParam(required = false) String search) {
        return cartService.getCart(userId, search);
    }
    @PutMapping("/{id}")
    public CartItem updateQuantity(@PathVariable Long id,
                                   @Valid @RequestBody QuantityRequest request) {
        return cartService.updateQuantity(id, request.quantity());
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable Long id) {
        cartService.removeItem(id);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}