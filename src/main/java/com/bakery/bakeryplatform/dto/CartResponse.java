package com.bakery.bakeryplatform.dto;
import com.bakery.bakeryplatform.model.CartItem;
import java.math.BigDecimal;
import java.util.List;

public record CartResponse(Long userId, List<CartItem> items, BigDecimal total) {
}