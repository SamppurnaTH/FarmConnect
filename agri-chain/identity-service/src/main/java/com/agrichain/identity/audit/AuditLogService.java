package com.agrichain.identity.audit;

import com.agrichain.identity.entity.AuditLog;
import com.agrichain.identity.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for retrieving append-only audit log entries.
 * Enforces Requirements 3.x.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Retrieves a paginated list of all audit logs.
     */
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Retrieves audit logs filtered by user ID.
     */
    public Page<AuditLog> getLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieves audit logs filtered by resource type and ID.
     */
    public Page<AuditLog> getLogsByResource(String resourceType, UUID resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable);
    }

    /**
     * Records a new audit log entry.
     */
    public AuditLog recordLog(AuditLog log) {
        return auditLogRepository.save(log);
    }
}
