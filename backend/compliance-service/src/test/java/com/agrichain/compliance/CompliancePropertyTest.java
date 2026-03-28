package com.agrichain.compliance;

import com.agrichain.common.enums.AuditStatus;
import com.agrichain.compliance.dto.ComplianceRecordRequest;
import com.agrichain.compliance.entity.Audit;
import com.agrichain.compliance.entity.CheckResult;
import com.agrichain.compliance.repository.AuditRepository;
import com.agrichain.compliance.repository.ComplianceRecordRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CompliancePropertyTest {

    private final ComplianceRecordRepository recordRepository = Mockito.mock(ComplianceRecordRepository.class);
    private final AuditRepository auditRepository = Mockito.mock(AuditRepository.class);
    private final ComplianceService complianceService;

    public CompliancePropertyTest() {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        when(builder.build()).thenReturn(restTemplate);
        this.complianceService = new ComplianceService(recordRepository, auditRepository, builder);
    }

    /**
     * Property 29: Compliance record is linked to the specified entity.
     */
    @Property(tries = 50)
    void property_29_record_linkage(
            @ForAll("entityTypes") String entityType,
            @ForAll("uuids") UUID entityId,
            @ForAll CheckResult result) {

        Mockito.reset(recordRepository);

        ComplianceRecordRequest req = new ComplianceRecordRequest();
        req.setEntityType(entityType);
        req.setEntityId(entityId);
        req.setCheckResult(result);
        req.setCheckDate(LocalDate.now());
        req.setCreatedBy(UUID.randomUUID());

        when(recordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        complianceService.createRecord(req);

        verify(recordRepository).save(argThat(record ->
            record.getEntityType().equals(entityType) &&
            record.getEntityId().equals(entityId) &&
            record.getCheckResult() == result
        ));
    }

    /**
     * Property 30: Audit record transitions correctly through its lifecycle.
     */
    @Property(tries = 50)
    void property_30_audit_lifecycle(
            @ForAll("uuids") UUID auditId,
            @ForAll AuditStatus currentStatus) {

        Mockito.reset(auditRepository);

        Audit audit = new Audit();
        audit.setId(auditId);
        audit.setStatus(currentStatus);
        when(auditRepository.findById(eq(auditId))).thenReturn(Optional.of(audit));
        when(auditRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        if (currentStatus != AuditStatus.In_Progress) {
            assertThatThrownBy(() -> complianceService.completeAudit(auditId, "Findings"))
                    .isInstanceOf(IllegalStateException.class);
        } else {
            assertThatCode(() -> complianceService.completeAudit(auditId, "Findings"))
                    .doesNotThrowAnyException();

            verify(auditRepository).save(argThat(a ->
                a.getStatus() == AuditStatus.Completed &&
                "Findings".equals(a.getFindings()) &&
                a.getCompletedAt() != null
            ));
        }
    }

    @Provide
    Arbitrary<UUID> uuids() {
        return Arbitraries.create(UUID::randomUUID);
    }

    @Provide
    Arbitrary<String> entityTypes() {
        return Arbitraries.of("Farmer", "CropListing", "Transaction", "SubsidyProgram");
    }
}
