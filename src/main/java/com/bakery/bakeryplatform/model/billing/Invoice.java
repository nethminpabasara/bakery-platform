package com.bakery.bakeryplatform.model.billing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "invoice_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Invoice {

    public enum EmailStatus {
        NOT_REQUESTED, NOT_CONFIGURED, PENDING, SENT, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", unique = true,nullable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "payment_id", unique = true)
    private Long paymentId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_type")
    private String paymentType;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    // Snapshot these values when the invoice is created, rather than using later edits.
    private String customerName;
    private String customerEmail;
    @Column(length = 10000)
    private String itemsSummary;
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(precision = 12, scale = 2)
    private BigDecimal paymentFee;
    @Enumerated(EnumType.STRING)
    private EmailStatus emailStatus = EmailStatus.NOT_REQUESTED;
    private LocalDateTime emailSentAt;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String value) { customerName = value; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String value) { customerEmail = value; }
    public String getItemsSummary() { return itemsSummary; }
    public void setItemsSummary(String value) { itemsSummary = value; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal value) { subtotal = value; }
    public BigDecimal getPaymentFee() { return paymentFee; }
    public void setPaymentFee(BigDecimal value) { paymentFee = value; }
    public EmailStatus getEmailStatus() {
        return emailStatus == null ? EmailStatus.NOT_REQUESTED : emailStatus;
    }
    public void setEmailStatus(EmailStatus value) { emailStatus = value; }
    public LocalDateTime getEmailSentAt() { return emailSentAt; }
    public void setEmailSentAt(LocalDateTime value) { emailSentAt = value; }

    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id = id;
    }

    public String getInvoiceNumber(){
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber){
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getInvoiceDate(){
        return invoiceDate;
    }
    public void setInvoiceDate(LocalDate invoiceDate){
        this.invoiceDate = invoiceDate;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public abstract double calculateTotal();

    private String customerPhone = null;
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String value) { customerPhone = value; }
    private String billingAddress = null;
    public String getBillingAddress() { return billingAddress; }
    public void setBillingAddress(String value) { billingAddress = value; }
    private String billingEmail = null;
    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String value) { billingEmail = value; }
    private String notes = null;
    public String getNotes() { return notes; }
    public void setNotes(String value) { notes = value; }
    private String status = "PAID";
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
}
