package com.agrichain.compliance;

import com.agrichain.compliance.dto.AuditRequest;
import com.agrichain.compliance.dto.ComplianceRecordRequest;
import com.agrichain.compliance.entity.ComplianceRecord;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/compliance")
public class ComplianceController {

    private final ComplianceService complianceService;

    public ComplianceController(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    /**
     * POST /compliance-records
     * Create compliance results.
     */
    @PostMapping("/records")
    public ResponseEntity<UUID> createRecord(@Valid @RequestBody ComplianceRecordRequest request) {
        UUID id = complianceService.createRecord(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /compliance-records
     * Query history for an entity.
     */
    @GetMapping("/records")
    public ResponseEntity<List<ComplianceRecord>> getRecords(@RequestParam String entityType, @RequestParam UUID entityId) {
        List<ComplianceRecord> records = complianceService.getRecords(entityType, entityId);
        return ResponseEntity.ok(records);
    }

    /**
     * POST /compliance/audits
     * Start an audit.
     */
    @PostMapping("/audits")
    public ResponseEntity<UUID> initiateAudit(@Valid @RequestBody AuditRequest request) {
        UUID id = complianceService.initiateAudit(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * PUT /compliance/audits/{id}/findings
     * Complete an audit.
     */
    @PutMapping("/audits/{id}/findings")
    public ResponseEntity<Void> completeAudit(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        String findings = body.get("findings");
        if (findings == null || findings.isBlank()) {
            throw new IllegalArgumentException("Findings must not be empty.");
        }
        complianceService.completeAudit(id, findings);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /compliance/audits/{id}/export
     * PDF export (mocked).
     */
    @GetMapping("/audits/{id}/export")
    public ResponseEntity<byte[]> exportAudit(@PathVariable UUID id) {
        byte[] pdf = complianceService.exportAudit(id);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "audit-report-" + id + ".pdf");
        
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStateError(IllegalStateException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }
}
