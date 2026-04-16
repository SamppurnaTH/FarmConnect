package com.agrichain.farmer.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for document verification requests (Market Officer action).
 */
public class DocumentVerifyRequest {

    @NotBlank(message = "Verification status is required")
    private String status;

    private String rejectionReason;

    public String getStatus()                    { return status; }
    public void setStatus(String status)         { this.status = status; }

    public String getRejectionReason()                       { return rejectionReason; }
    public void setRejectionReason(String rejectionReason)   { this.rejectionReason = rejectionReason; }
}
