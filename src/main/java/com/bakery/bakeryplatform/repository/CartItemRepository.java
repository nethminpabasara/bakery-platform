package com.bakery.bakeryplatform.repository;

import com.bakery.bakeryplatform.model.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findByUserIdAndProductId(Long userId, String productId);

    List<CartItem> findByUserIdAndProductNameContainingIgnoreCase(Long userId, String name);

    void deleteByUserId(Long userId);
}