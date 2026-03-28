package com.agrichain.reporting.dto;

import java.math.BigDecimal;

/**
 * DTO for the KPI dashboard response.
 */
public class DashboardResponse {

    private long activeFarmerCount;
    private long totalCropVolume;
    private BigDecimal totalTransactionValue;
    private BigDecimal totalSubsidiesDisbursed;

    // Getters and setters
    public long getActiveFarmerCount() { return activeFarmerCount; }
    public void setActiveFarmerCount(long activeFarmerCount) { this.activeFarmerCount = activeFarmerCount; }
    public long getTotalCropVolume() { return totalCropVolume; }
    public void setTotalCropVolume(long totalCropVolume) { this.totalCropVolume = totalCropVolume; }
    public BigDecimal getTotalTransactionValue() { return totalTransactionValue; }
    public void setTotalTransactionValue(BigDecimal totalTransactionValue) { this.totalTransactionValue = totalTransactionValue; }
    public BigDecimal getTotalSubsidiesDisbursed() { return totalSubsidiesDisbursed; }
    public void setTotalSubsidiesDisbursed(BigDecimal totalSubsidiesDisbursed) { this.totalSubsidiesDisbursed = totalSubsidiesDisbursed; }
}
