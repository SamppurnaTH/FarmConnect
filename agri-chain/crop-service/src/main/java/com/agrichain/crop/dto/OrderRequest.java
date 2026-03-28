package com.agrichain.crop.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for placing a new order.
 */
public class OrderRequest {

    @NotNull
    private UUID listingId;

    @NotNull
    private UUID buyerId;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotBlank
    @Size(max = 500)
    private String deliveryAddress;

    // Getters and setters
    public UUID getListingId() { return listingId; }
    public void setListingId(UUID listingId) { this.listingId = listingId; }
    public UUID getBuyerId() { return buyerId; }
    public void setBuyerId(UUID buyerId) { this.buyerId = buyerId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
