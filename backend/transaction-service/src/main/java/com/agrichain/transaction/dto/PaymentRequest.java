package com.agrichain.transaction.dto;

import com.agrichain.transaction.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * DTO for submitting a payment.
 */
public class PaymentRequest {

    @NotNull
    private UUID transactionId;

    @NotNull
    private PaymentMethod method;

    @Size(max = 255)
    private String gatewayRef;

    // Getters and setters
    public UUID getTransactionId() { return transactionId; }
    public void setTransactionId(UUID transactionId) { this.transactionId = transactionId; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public String getGatewayRef() { return gatewayRef; }
    public void setGatewayRef(String gatewayRef) { this.gatewayRef = gatewayRef; }
}
