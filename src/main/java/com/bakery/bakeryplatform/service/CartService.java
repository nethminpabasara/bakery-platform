package com.bakery.bakeryplatform.service;

import com.bakery.bakeryplatform.dto.CartResponse;
import com.bakery.bakeryplatform.exception.BusinessRuleException;
import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.repository.CartItemRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartItemRepository cartItemRepository;

    public CartService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    // add an item to the cart
    public CartItem addToCart(CartItem newItem) {
        CartItem oldItem = cartItemRepository.findByUserIdAndProductId(newItem.getUserId(), newItem.getProductId()).orElse(null);

        // the product is not in the cart yet, so save it as a new row
        if (oldItem == null) {
            return cartItemRepository.save(newItem);
        }

        // the product is already in the cart, so add the quantities
        int total = oldItem.getQuantity() + newItem.getQuantity();
        if (total > 50) {
            throw new BusinessRuleException("You can only have 50 of the same item in the cart.");
        }
        oldItem.setQuantity(total);
        return cartItemRepository.save(oldItem);
    }

    // Read
    public CartResponse getCart(Long userId, String search) {
        List<CartItem> items;

        if (search == null || search.isBlank()) {
            items = cartItemRepository.findByUserId(userId);
        } else {
            items = cartItemRepository.findByUserIdAndProductNameContainingIgnoreCase(userId, search);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : items) {
            total = total.add(item.getTotal());
        }

        return new CartResponse(userId, items, total);
    }

    // Update
    public CartItem updateQuantity(Long id, int quantity) {
        CartItem item = findItem(id);
        item.setQuantity(quantity);
        return cartItemRepository.save(item);
    }

    //Delete
    public void removeItem(Long id) {
        CartItem item = findItem(id);
        cartItemRepository.delete(item);
    }

    // remove all items of one user
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    // find one item or show a "not found" error
    private CartItem findItem(Long id) {
        CartItem item = cartItemRepository.findById(id).orElse(null);
        if (item == null) {
            throw new ResourceNotFoundException("Cart item not found with id " + id);
        }
        return item;
    }
}