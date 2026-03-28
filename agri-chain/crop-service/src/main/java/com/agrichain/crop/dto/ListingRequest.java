package com.agrichain.crop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new crop listing.
 */
public class ListingRequest {

    @NotNull
    private UUID farmerId;

    @NotBlank
    @Size(max = 100)
    private String cropType;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    @Positive
    private BigDecimal pricePerUnit;

    @NotBlank
    @Size(max = 255)
    private String location;

    // Getters and setters
    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }
    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(BigDecimal pricePerUnit) { this.pricePerUnit = pricePerUnit; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
