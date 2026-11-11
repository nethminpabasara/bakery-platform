package com.bakery.bakeryplatform.model.billing;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("PAYMENT")
public class PaymentInvoice extends Invoice {
    @Override
    public double calculateTotal() {
        return getAmount() == null ? 0 : getAmount().doubleValue();
    }
}
