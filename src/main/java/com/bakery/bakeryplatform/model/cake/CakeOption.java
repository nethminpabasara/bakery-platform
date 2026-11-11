package com.bakery.bakeryplatform.model.cake;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "option_type")
public abstract class CakeOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Encapsulation
    private String name;
    private double price;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getOptionType() {
        if (this instanceof Flavour) {
            return "FLAVOUR";
        }
        if (this instanceof Size) {
            return "SHAPE";
        }
        if (this instanceof Topping) {
            return "DECORATION";
        }
        throw new IllegalStateException("Unsupported cake option type: " + getClass().getName());
    }

    // Abstraction & Polymorphism
    public abstract double getExtraPrice();

    private boolean available = true;
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean value) { available = value; }
    private Double configuredExtraPrice = null;
    public Double getConfiguredExtraPrice() { return configuredExtraPrice; }
    public void setConfiguredExtraPrice(Double value) { configuredExtraPrice = value; }
}