package com.bakery.bakeryplatform.model.cake;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("SIZE")
public class Size extends CakeOption {

    @Override
    public double getExtraPrice() {
        if (getConfiguredExtraPrice() != null) return getConfiguredExtraPrice();
        // Polymorphism
        return getPrice() * 1.5;
    }
}