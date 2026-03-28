package com.agrichain.transaction.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for creating a new transaction.
 */
public class TransactionRequest {

    @NotNull
    private UUID orderId;

    @NotNull
    @Positive
    private BigDecimal amount;

    // Getters and setters
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
