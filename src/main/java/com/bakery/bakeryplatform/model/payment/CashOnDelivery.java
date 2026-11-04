package com.bakery.bakeryplatform.model.payment;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue("CASH_ON_DELIVERY")
public class CashOnDelivery extends Payment {

    public CashOnDelivery() {
    }

    public CashOnDelivery(Long orderId, BigDecimal amount) {
        super(orderId, amount);
    }

    @Override
    public BigDecimal calculateFee() {
        // COD has a flat fee
        return new BigDecimal("100.00");
    }

    @Override
    public String processPayment() {
        return "Please pay Rs. " + getAmount().add(getFee()).toPlainString() + " in cash when the order is delivered.";
    }

    @Override
    public String getPaymentType() {
        return "CASH_ON_DELIVERY";
    }
}