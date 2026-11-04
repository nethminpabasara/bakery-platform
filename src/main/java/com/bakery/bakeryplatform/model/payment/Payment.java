package com.bakery.bakeryplatform.model.payment;

import com.bakery.bakeryplatform.model.BaseEntity;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)   // card and cash payment share ONE table
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment extends BaseEntity {

    private Long orderId;
    private BigDecimal amount;      // the order total
    private BigDecimal fee;         // calculated by the child class
    private String receiptNote;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PAID;

    protected Payment() {
    }

    protected Payment(Long orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    // abstraction + polymorphism
    public abstract BigDecimal calculateFee();

    public abstract String processPayment();

    public abstract String getPaymentType();

    public BigDecimal getTotalCharged() {
        if (amount == null || fee == null) {
            return amount;
        }
        return amount.add(fee);
    }

    public Long getOrderId() {
        return orderId;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }
    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getReceiptNote() {
        return receiptNote;
    }
    public void setReceiptNote(String receiptNote) {
        this.receiptNote = receiptNote;
    }

    public PaymentStatus getStatus() {
        return status;
    }
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    @Override
    public String describe() {
        return "Payment #" + getId() + " for order #" + orderId + " (" + status + ")";
    }
}
