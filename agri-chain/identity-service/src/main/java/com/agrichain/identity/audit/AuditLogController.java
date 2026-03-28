package com.agrichain.identity.audit;

import com.agrichain.identity.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * GET /audit-log — Paginated query for Compliance_Officer and Government_Auditor.
 * 
 * Supports filtering by userId or resourceType/resourceId.
 * Standard Spring Data JPA pagination (page, size, sort).
 *
 * @return 200 with paginated audit logs on success
 * @return 403 if the caller is not a Compliance_Officer or Government_Auditor
 */
@RestController
@RequestMapping("/audit-log")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Compliance_Officer', 'Government_Auditor')")
    public ResponseEntity<Page<AuditLog>> queryAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) UUID resourceId,
            Pageable pageable) {

        Page<AuditLog> result;

        if (userId != null) {
            result = auditLogService.getLogsByUser(userId, pageable);
        } else if (resourceType != null && resourceId != null) {
            result = auditLogService.getLogsByResource(resourceType, resourceId, pageable);
        } else {
            result = auditLogService.getAllLogs(pageable);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-resource")
    public ResponseEntity<Page<AuditLog>> getLogsByResource(@RequestParam String resourceType, 
                                                           @RequestParam UUID resourceId,
                                                           Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogsByResource(resourceType, resourceId, pageable));
    }

    /**
     * POST /audit-log
     * Internal endpoint for recording state-changing actions across services.
     */
    @PostMapping
    public ResponseEntity<AuditLog> recordLog(@RequestBody AuditLog log) {
        return ResponseEntity.status(201).body(auditLogService.recordLog(log));
    }
}
