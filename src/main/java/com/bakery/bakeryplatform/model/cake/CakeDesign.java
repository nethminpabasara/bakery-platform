package com.bakery.bakeryplatform.model.cake;

import jakarta.persistence.*;

@Entity
@Table(name = "cake_designs")
public class CakeDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Encapsulation
    private String shape;
    private String flavor;
    private String decoration;
    private String filling;
    private String cakeMessage;
    private String referenceImageUrl;
    private double estimatedPrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShape() { return shape; }
    public void setShape(String shape) { this.shape = shape; }

    public String getFlavor() { return flavor; }
    public void setFlavor(String flavor) { this.flavor = flavor; }

    public String getDecoration() { return decoration; }
    public void setDecoration(String decoration) { this.decoration = decoration; }

    public String getFilling() { return filling; }
    public void setFilling(String filling) { this.filling = filling; }

    public String getCakeMessage() { return cakeMessage; }
    public void setCakeMessage(String cakeMessage) { this.cakeMessage = cakeMessage; }

    public String getReferenceImageUrl() { return referenceImageUrl; }
    public void setReferenceImageUrl(String referenceImageUrl) { this.referenceImageUrl = referenceImageUrl; }

    public double getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(double estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    private Long userId = null;
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    private String colour = null;
    public String getColour() { return colour; }
    public void setColour(String value) { colour = value; }
    private Integer layers = 1;
    public Integer getLayers() { return layers; }
    public void setLayers(Integer value) { layers = value; }
    private String cakeSize = "1.5 kg";
    public String getCakeSize() { return cakeSize; }
    public void setCakeSize(String value) { cakeSize = value; }
}