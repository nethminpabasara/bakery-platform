package com.bakery.bakeryplatform.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentRequest(
        @NotNull(message = "Order id is required")
        Long orderId,

        @NotBlank(message = "Payment type is required")
        @Pattern(regexp = "CARD|CASH_ON_DELIVERY", message = "Payment type must be CARD or CASH_ON_DELIVERY")
        String paymentType,

        @Size(max = 60, message = "Card holder name must be 60 characters or less")
        String cardHolderName,

        @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
        String cardNumber) {
}