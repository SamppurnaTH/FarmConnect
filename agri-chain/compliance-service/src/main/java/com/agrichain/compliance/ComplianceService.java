package com.agrichain.compliance;

import com.agrichain.common.enums.AuditStatus;
import com.agrichain.compliance.dto.AuditRequest;
import com.agrichain.compliance.dto.ComplianceRecordRequest;
import com.agrichain.compliance.entity.Audit;
import com.agrichain.compliance.entity.CheckResult;
import com.agrichain.compliance.entity.ComplianceRecord;
import com.agrichain.compliance.repository.AuditRepository;
import com.agrichain.compliance.repository.ComplianceRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ComplianceService {

    private final ComplianceRecordRepository recordRepository;
    private final AuditRepository auditRepository;
    private final RestTemplate restTemplate;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    public ComplianceService(ComplianceRecordRepository recordRepository, 
                             AuditRepository auditRepository,
                             RestTemplateBuilder restTemplateBuilder) {
        this.recordRepository = recordRepository;
        this.auditRepository = auditRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 13.1: Create compliance record.
     */
    @Transactional
    public UUID createRecord(ComplianceRecordRequest request) {
        ComplianceRecord record = new ComplianceRecord();
        record.setEntityType(request.getEntityType());
        record.setEntityId(request.getEntityId());
        record.setCheckResult(request.getCheckResult());
        record.setCheckDate(request.getCheckDate());
        record.setNotes(request.getNotes());
        record.setCreatedBy(request.getCreatedBy());

        UUID id = recordRepository.save(record).getId();

        // Requirement 13.3: Notify on Fail result
        if (request.getCheckResult() == CheckResult.Fail) {
            notifyFailure(request);
        }

        return id;
    }

    public List<ComplianceRecord> getRecords(String entityType, UUID entityId) {
        return recordRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Requirement 14.1: Initiate audit.
     */
    @Transactional
    public UUID initiateAudit(AuditRequest request) {
        Audit audit = new Audit();
        audit.setScope(request.getScope());
        audit.setInitiatedBy(request.getInitiatedBy());
        audit.setStatus(AuditStatus.In_Progress);
        
        return auditRepository.save(audit).getId();
    }

    /**
     * Requirement 14.3: Submit findings and complete audit.
     */
    @Transactional
    public void completeAudit(UUID auditId, String findings) {
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found"));

        if (audit.getStatus() != AuditStatus.In_Progress) {
            throw new IllegalStateException("Audit is already completed or cancelled.");
        }

        audit.setFindings(findings);
        audit.setStatus(AuditStatus.Completed);
        audit.setCompletedAt(Instant.now());

        auditRepository.save(audit);
    }

    /**
     * Requirement 14.4: Export audit report.
     */
    public byte[] exportAudit(UUID auditId) {
        Audit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new IllegalArgumentException("Audit not found"));

        // Placeholder for PDF generation
        String report = "Audit Report\nID: " + audit.getId() + "\nStatus: " + audit.getStatus() + 
                       "\nFindings: " + audit.getFindings();
        return report.getBytes();
    }

    private void notifyFailure(ComplianceRecordRequest request) {
        try {
            Map<String, Object> notification = Map.of(
                "userId", request.getCreatedBy(), // Simplified: In reality, find owner of entityId
                "channel", "In_App",
                "content", "COMPLIANCE FAILURE: " + request.getEntityType() + " [" + request.getEntityId() + "] failed check on " + request.getCheckDate()
            );
            restTemplate.postForObject(notificationServiceUrl + "/notifications", notification, Void.class);
        } catch (Exception e) {
            // Log warning but don't fail the transaction
            System.err.println("Failed to send compliance failure notification: " + e.getMessage());
        }
    }
}
