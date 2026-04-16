package com.agrichain.subsidy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for rejecting a disbursement with a mandatory reason.
 */
public class DisbursementRejectRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(max = 500)
    private String reason;

    public String getReason()              { return reason; }
    public void setReason(String reason)   { this.reason = reason; }
}
