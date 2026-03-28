package com.agrichain.reporting.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request for generating a scoped report.
 */
public class ReportRequest {

    @NotBlank
    private String scope; // e.g., "TRANSACTIONS", "SUBSIDIES"

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotBlank
    private String format; // "CSV" or "PDF"

    // Getters and setters
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}
