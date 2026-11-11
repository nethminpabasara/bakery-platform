package com.bakery.bakeryplatform.dto.payment;

import com.bakery.bakeryplatform.model.payment.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusRequest(
        @NotNull(message = "Status is required")
        PaymentStatus status) {
}