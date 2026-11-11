package com.bakery.bakeryplatform.model.billing;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

@Entity
@Table(name = "refund_requests")
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "status")
    private String status;

    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id = id;
    }
    public Long getOrderId(){
        return orderId;
    }
    public void setOrderId(Long orderId){
        this.orderId = orderId;
    }
    public String getReason(){
        return reason;
    }
    public void setReason(String reason){
        this.reason = reason;
    }
    public LocalDate getRequestDate(){
        return requestDate;
    }
    public void setRequestDate(LocalDate requestDate){
        this.requestDate = requestDate;
    }
    public  void setStatus(String status){
        this.status = status;
    }

    public String getStatus() {
        return status;
    }


    private String adminNotes = null;
    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String value) { adminNotes = value; }
}