package com.bakery.bakeryplatform.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
        @NotNull(message = "User id is required")
        Long userId,

        @Size(max = 200, message = "Note must be 200 characters or less")
        String note) {
}