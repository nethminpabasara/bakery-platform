package com.bakery.bakeryplatform.model.catalogue;


public class Item {
    // Encapsulation
    private String id;
    private String name;
    private String description;
    private String flavor;
    private String size;
    private double price;
    private int shape;
    private String category;
    private String availability;
    private String imageUrl;

    public Item() {
    }
    // Constructor with parameters
    public Item(String id, String name, String description, String flavor, String size, double price, int shape, String category, String availability, String imageUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.flavor = flavor;
        this.size = size;
        this.price = price;
        this.shape = shape;
        this.category = category;
        this.availability = availability;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFlavor() {
        return flavor;
    }

    public void setFlavor(String flavor) {
        this.flavor = flavor;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getShape() {
        return shape;
    }

    public void setShape(int shape) {
        this.shape = shape;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    private java.util.Map<String, Object> website = new java.util.LinkedHashMap<>();
    public java.util.Map<String, Object> getWebsite() { return website; }
    public void setWebsite(java.util.Map<String, Object> value) { website = value; }
}

