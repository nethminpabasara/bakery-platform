package com.bakery.bakeryplatform.model.cake;

import jakarta.persistence.*;

@Entity
@Table(name = "custom_bookings")
public class CustomCakeBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    // Encapsulation
    private String customerName;
    private String contactNumber;
    private String customerEmail;
    private String requiredDate;
    private String preferredTime;
    private String occasion;
    private String shape;
    private String flavor;
    private String decoration;
    private String filling;
    private String cakeSize;
    private String cakeMessage;
    private String pickupOrDelivery;
    private String deliveryAddress;
    private String referenceImageUrl;
    private String promoCode;
    private String notes;
    private double estimatedPrice;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getRequiredDate() { return requiredDate; }
    public void setRequiredDate(String requiredDate) { this.requiredDate = requiredDate; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }

    public String getDecoration() { return decoration; }
    public void setDecoration(String decoration) { this.decoration = decoration; }

    public String getFilling() { return filling; }
    public void setFilling(String filling) { this.filling = filling; }

    public String getCakeSize() { return cakeSize; }
    public void setCakeSize(String cakeSize) { this.cakeSize = cakeSize; }

    public String getCakeMessage() { return cakeMessage; }
    public void setCakeMessage(String cakeMessage) { this.cakeMessage = cakeMessage; }

    public String getPickupOrDelivery() { return pickupOrDelivery; }
    public void setPickupOrDelivery(String pickupOrDelivery) { this.pickupOrDelivery = pickupOrDelivery; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getReferenceImageUrl() { return referenceImageUrl; }
    public void setReferenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    private Long userId = null;
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    private Integer servings=35;
    public Integer getServings(){return servings;}
    public void setServings(Integer value){servings=value;}
}