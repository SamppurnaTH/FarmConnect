package com.agrichain.reporting.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "report_metadata")
public class ReportMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope", length = 255, nullable = false)
    private String scope;

    @Column(name = "generated_by", nullable = false)
    private UUID generatedBy;

    @Column(name = "generation_timestamp", nullable = false, updatable = false)
    private Instant generationTimestamp;

    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;

    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;

    @Column(name = "format", length = 10, nullable = false)
    private String format;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        if (generationTimestamp == null) {
            generationTimestamp = createdAt;
        }
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public UUID getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(UUID generatedBy) { this.generatedBy = generatedBy; }

    public Instant getGenerationTimestamp() { return generationTimestamp; }
    public void setGenerationTimestamp(Instant generationTimestamp) { this.generationTimestamp = generationTimestamp; }

    public LocalDate getDateRangeStart() { return dateRangeStart; }
    public void setDateRangeStart(LocalDate dateRangeStart) { this.dateRangeStart = dateRangeStart; }

    public LocalDate getDateRangeEnd() { return dateRangeEnd; }
    public void setDateRangeEnd(LocalDate dateRangeEnd) { this.dateRangeEnd = dateRangeEnd; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Instant getCreatedAt() { return createdAt; }
}
