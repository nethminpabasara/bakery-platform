package com.bakery.bakeryplatform.model.user;


import jakarta.persistence.*;

@Entity

public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String recipientName;
    private String address;
    private String status; // Pending, Out for Delivery, Delivered

    // The customer who made this delivery (used by the admin Delivery tab)
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    private Long orderId = null;
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    private String deliveryDate = null;
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String value) { deliveryDate = value; }
}
