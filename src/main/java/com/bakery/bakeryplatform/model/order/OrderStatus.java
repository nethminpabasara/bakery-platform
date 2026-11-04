package com.bakery.bakeryplatform.model.order;

public enum OrderStatus {
    PENDING,
    PREPARING,
    READY,
    DELIVERED;


    public boolean canMoveTo(OrderStatus next) {
        return next.ordinal() == this.ordinal() + 1;
    }
}
