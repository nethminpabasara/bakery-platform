package com.bakery.bakeryplatform.model.catalogue;
import jakarta.persistence.*;
@Entity @Table(name="website_stock")
public class SiteStock {
    @Id private String productId;
    private int quantity;
    @Version private Long version;
    public SiteStock(){}
    public SiteStock(String productId,int quantity){this.productId=productId;this.quantity=quantity;}
    public String getProductId(){return productId;}
    public int getQuantity(){return quantity;} public void setQuantity(int v){quantity=v;}
}
