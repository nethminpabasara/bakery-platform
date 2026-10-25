package com.bakery.bakeryplatform.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {
    @NotNull(message = "User id is required")
    private Long userId;

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must be 100 characters or less")
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 50, message = "Quantity cannot be more than 50")
    private int quantity;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.01", message = "Unit price must be more than 0")
    private BigDecimal unitPrice;

    public CartItem() {
    }

    public CartItem(Long userId, Long productId, String productName, int quantity, BigDecimal unitPrice) {
        this.userId = userId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {

        this.userId = userId;
    }
    public Long getProductId() {

        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public String getProductName() {
        return productName;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public int getQuantity() {
        return quantity;
    }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }


    // not stored in the database, calculated every time
    public BigDecimal getTotal() {
        if (unitPrice == null) {
            return BigDecimal.ZERO;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Override
    public String describe() {
        return quantity + " x " + productName + " = Rs. " + getTotal();
    }
}