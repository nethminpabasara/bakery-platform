package com.bakery.bakeryplatform.model.user;

import jakarta.persistence.Entity;

@Entity
public class Customer extends User {
    private String phoneNumber;
    private String address;


    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String getRole() {
        return "CUSTOMER";
    }
}