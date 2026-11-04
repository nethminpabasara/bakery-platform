package com.bakery.bakeryplatform.model.payment;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {

    private String cardHolderName;
    private String cardLast4;       //store last 4 number in the card

    public CardPayment() {
    }

    public CardPayment(Long orderId, BigDecimal amount, String cardHolderName, String cardLast4) {
        super(orderId, amount);
        this.cardHolderName = cardHolderName;
        this.cardLast4 = cardLast4;
    }

    @Override
    public BigDecimal calculateFee() {
        // card fee is 2% of the amount
        return getAmount().multiply(new BigDecimal("0.02")).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String processPayment() {
        return "Rs. " + getAmount().add(getFee()).toPlainString() + " charged to the card ending " + cardLast4 + ".";
    }

    @Override
    public String getPaymentType() {
        return "CARD";
    }

    public String getCardHolderName() {
        return cardHolderName;
    }
    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getCardLast4() {
        return cardLast4;
    }
    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }
}