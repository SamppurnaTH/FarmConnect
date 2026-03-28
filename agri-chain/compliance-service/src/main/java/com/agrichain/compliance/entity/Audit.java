package com.agrichain.compliance.entity;

import com.agrichain.common.enums.AuditStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audits")
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "scope", columnDefinition = "text", nullable = false)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuditStatus status;

    @Column(name = "findings", columnDefinition = "text")
    private String findings;

    @Column(name = "initiated_by", nullable = false)
    private UUID initiatedBy;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void prePersist() {
        initiatedAt = Instant.now();
        if (status == null) {
            status = AuditStatus.In_Progress;
        }
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public AuditStatus getStatus() { return status; }
    public void setStatus(AuditStatus status) { this.status = status; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public UUID getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(UUID initiatedBy) { this.initiatedBy = initiatedBy; }

    public Instant getInitiatedAt() { return initiatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
