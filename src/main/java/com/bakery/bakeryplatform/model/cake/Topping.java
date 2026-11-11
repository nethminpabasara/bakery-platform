package com.bakery.bakeryplatform.model.cake;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DECORATION")
public class Topping extends CakeOption {

    @Override
    public double getExtraPrice() {
        if (getConfiguredExtraPrice() != null) return getConfiguredExtraPrice();
        // Polymorphism
        return getPrice() + 300.0;
    }
}