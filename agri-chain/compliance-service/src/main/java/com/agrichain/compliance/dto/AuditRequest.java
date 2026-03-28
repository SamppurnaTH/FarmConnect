package com.agrichain.compliance.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

/**
 * DTO for initiating a formal audit.
 */
public class AuditRequest {

    @NotBlank
    private String scope;

    @NotNull
    private UUID initiatedBy;

    // Getters and setters
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public UUID getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(UUID initiatedBy) { this.initiatedBy = initiatedBy; }
}
