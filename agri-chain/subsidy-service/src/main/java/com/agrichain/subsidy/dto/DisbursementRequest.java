package com.agrichain.subsidy.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for requesting a subsidy disbursement.
 */
public class DisbursementRequest {

    @NotNull
    private UUID programId;

    @NotNull
    private UUID farmerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    @Size(max = 50)
    private String programCycle;

    // Getters and setters
    public UUID getProgramId() { return programId; }
    public void setProgramId(UUID programId) { this.programId = programId; }
    public UUID getFarmerId() { return farmerId; }
    public void setFarmerId(UUID farmerId) { this.farmerId = farmerId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getProgramCycle() { return programCycle; }
    public void setProgramCycle(String programCycle) { this.programCycle = programCycle; }
}
