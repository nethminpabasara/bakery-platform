package com.bakery.bakeryplatform.model.billing;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BOOKING")
public class BookingInvoice extends Invoice{

    private double bookingAmount;
    private double advancedPayment;

    public double getBookingAmount(){
        return bookingAmount;
    }
    public void setBookingAmount(double bookingAmount){

        this.bookingAmount = bookingAmount;
    }
    public double getAdvancedPayment(){

        return advancedPayment;
    }
    public void setAdvancedPayment(double advancedPayment){
        this.advancedPayment = advancedPayment;
    }

    @Override
    public double calculateTotal(){

        return bookingAmount - advancedPayment;
    }
}