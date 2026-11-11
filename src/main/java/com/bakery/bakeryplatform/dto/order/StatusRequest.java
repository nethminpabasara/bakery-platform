package com.bakery.bakeryplatform.dto.order;


import com.bakery.bakeryplatform.model.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status) {
}