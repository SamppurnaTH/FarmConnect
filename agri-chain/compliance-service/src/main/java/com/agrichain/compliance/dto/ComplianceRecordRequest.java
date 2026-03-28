package com.agrichain.compliance.dto;

import com.agrichain.compliance.entity.CheckResult;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for creating a new compliance record.
 */
public class ComplianceRecordRequest {

    @NotBlank
    @Size(max = 100)
    private String entityType;

    @NotNull
    private UUID entityId;

    @NotNull
    private CheckResult checkResult;

    @NotNull
    private LocalDate checkDate;

    private String notes;

    @NotNull
    private UUID createdBy;

    // Getters and setters
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public CheckResult getCheckResult() { return checkResult; }
    public void setCheckResult(CheckResult checkResult) { this.checkResult = checkResult; }
    public LocalDate getCheckDate() { return checkDate; }
    public void setCheckDate(LocalDate checkDate) { this.checkDate = checkDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
}
