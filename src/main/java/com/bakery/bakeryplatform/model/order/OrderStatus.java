package com.bakery.bakeryplatform.model.order;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;


    public boolean canMoveTo(OrderStatus next) {
        if(next == null || this == DELIVERED || this == CANCELLED)return false;
        if(next == CANCELLED)return this == PENDING || this == CONFIRMED;
        return switch(this){
            case PENDING -> next == CONFIRMED || next == PREPARING;
            case CONFIRMED -> next == PREPARING;
            case PREPARING -> next == READY;
            case READY -> next == OUT_FOR_DELIVERY || next == DELIVERED;
            case OUT_FOR_DELIVERY -> next == DELIVERED;
            default -> false;
        };
    }
}
