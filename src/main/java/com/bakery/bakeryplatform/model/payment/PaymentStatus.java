package com.bakery.bakeryplatform.model.payment;


public enum PaymentStatus {
    PENDING,
    PAID,
    REFUNDED;

    // paid payment can refund
    public boolean canMoveTo(PaymentStatus next) {
        return (this == PENDING && next == PAID)
                || (this == PAID && next == REFUNDED);
    }
}
