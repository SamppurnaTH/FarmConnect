package com.agrichain.subsidy;

import com.agrichain.common.enums.DisbursementStatus;
import com.agrichain.common.enums.SubsidyProgramStatus;
import com.agrichain.subsidy.dto.DisbursementRequest;
import com.agrichain.subsidy.dto.ProgramRequest;
import com.agrichain.subsidy.entity.Disbursement;
import com.agrichain.subsidy.entity.SubsidyProgram;
import com.agrichain.subsidy.repository.DisbursementRepository;
import com.agrichain.subsidy.repository.SubsidyProgramRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SubsidyService {

    private final SubsidyProgramRepository programRepository;
    private final DisbursementRepository disbursementRepository;
    private final RestTemplate restTemplate;

    @Value("${services.farmer.url:http://localhost:8082}")
    private String farmerServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    public SubsidyService(SubsidyProgramRepository programRepository,
                          DisbursementRepository disbursementRepository,
                          RestTemplateBuilder restTemplateBuilder) {
        this.programRepository      = programRepository;
        this.disbursementRepository = disbursementRepository;
        this.restTemplate           = restTemplateBuilder.build();
    }

    // ── Programs ──────────────────────────────────────────────────────────────

    /**
     * Requirement 11.1: Create subsidy program (Draft).
     */
    @Transactional
    public UUID createProgram(ProgramRequest request) {
        SubsidyProgram program = new SubsidyProgram();
        program.setTitle(request.getTitle());
        program.setDescription(request.getDescription());
        program.setStartDate(request.getStartDate());
        program.setEndDate(request.getEndDate());
        program.setBudgetAmount(request.getBudgetAmount());
        program.setCreatedBy(request.getCreatedBy());
        program.setStatus(SubsidyProgramStatus.Draft);
        return programRepository.save(program).getId();
    }

    /**
     * Requirement 11.2: Activate / close program. Monotonic transitions only.
     */
    @Transactional
    public void updateProgramStatus(UUID id, SubsidyProgramStatus newStatus) {
        SubsidyProgram program = programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));

        if (program.getStatus() == SubsidyProgramStatus.Closed) {
            throw new IllegalStateException("Closed programs cannot be modified.");
        }
        if (newStatus == SubsidyProgramStatus.Active && program.getStatus() != SubsidyProgramStatus.Draft) {
            throw new IllegalStateException("Only Draft programs can be activated.");
        }
        if (newStatus == SubsidyProgramStatus.Draft) {
            throw new IllegalStateException("Cannot move program back to Draft.");
        }

        program.setStatus(newStatus);
        programRepository.save(program);
    }

    public List<SubsidyProgram> listPrograms() {
        return programRepository.findAll();
    }

    public SubsidyProgram getProgram(UUID id) {
        return programRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
    }

    // ── Disbursements ─────────────────────────────────────────────────────────

    /**
     * Requirement 11.4: Create disbursement.
     * Enforces budget, program status, farmer eligibility, and uniqueness.
     */
    @Transactional
    public UUID applyForDisbursement(DisbursementRequest request) {
        SubsidyProgram program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));

        if (program.getStatus() != SubsidyProgramStatus.Active) {
            throw new IllegalStateException("Disbursements only allowed for Active programs.");
        }

        BigDecimal remainingBudget = program.getBudgetAmount().subtract(program.getTotalDisbursed());
        if (request.getAmount().compareTo(remainingBudget) > 0) {
            throw new IllegalArgumentException("Disbursement exceeds program budget.");
        }

        // Verify farmer is Active — separate try/catch so IllegalStateException propagates correctly
        verifyFarmerIsActive(request.getFarmerId());

        if (disbursementRepository.existsByFarmerIdAndProgramIdAndProgramCycle(
                request.getFarmerId(), request.getProgramId(), request.getProgramCycle())) {
            throw new IllegalStateException("Duplicate disbursement for this program cycle.");
        }

        Disbursement disbursement = new Disbursement();
        disbursement.setProgramId(request.getProgramId());
        disbursement.setFarmerId(request.getFarmerId());
        disbursement.setAmount(request.getAmount());
        disbursement.setProgramCycle(request.getProgramCycle());
        disbursement.setStatus(DisbursementStatus.Pending);

        // Pre-allocate budget
        program.setTotalDisbursed(program.getTotalDisbursed().add(request.getAmount()));
        programRepository.save(program);

        return disbursementRepository.save(disbursement).getId();
    }

    /**
     * Requirement 12.2: Approve disbursement.
     * reviewerId is extracted from JWT in the controller — never random.
     */
    @Transactional
    public void approveDisbursement(UUID id, UUID reviewerId) {
        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found"));

        if (disbursement.getStatus() != DisbursementStatus.Pending) {
            throw new IllegalStateException("Only Pending disbursements can be approved.");
        }

        disbursement.setStatus(DisbursementStatus.Approved);
        disbursement.setApprovedBy(reviewerId);
        disbursement.setApprovedAt(Instant.now());
        disbursementRepository.save(disbursement);

        // Notify farmer
        sendNotification(disbursement.getFarmerId(),
                "Your subsidy disbursement of $" + disbursement.getAmount()
                + " for program cycle '" + disbursement.getProgramCycle() + "' has been approved.");
    }

    /**
     * Reject a disbursement with a mandatory reason.
     * Reconciles the pre-allocated budget back to the program.
     */
    @Transactional
    public void rejectDisbursement(UUID id, UUID reviewerId, String reason) {
        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found"));

        if (disbursement.getStatus() != DisbursementStatus.Pending) {
            throw new IllegalStateException("Only Pending disbursements can be rejected.");
        }

        disbursement.setStatus(DisbursementStatus.Rejected);
        disbursement.setApprovedBy(reviewerId);
        disbursement.setApprovedAt(Instant.now());
        disbursement.setRejectionReason(reason);
        disbursementRepository.save(disbursement);

        // Reconcile budget — return the pre-allocated amount back to the program
        programRepository.findById(disbursement.getProgramId()).ifPresent(program -> {
            BigDecimal reconciled = program.getTotalDisbursed().subtract(disbursement.getAmount());
            program.setTotalDisbursed(reconciled.max(BigDecimal.ZERO));
            programRepository.save(program);
        });

        // Notify farmer
        sendNotification(disbursement.getFarmerId(),
                "Your subsidy disbursement request for program cycle '"
                + disbursement.getProgramCycle() + "' was rejected. Reason: " + reason);
    }

    public List<Disbursement> listDisbursements() {
        return disbursementRepository.findAll();
    }

    /**
     * Uses a SUM aggregate query — does NOT load all programs into memory.
     */
    public BigDecimal getTotalDisbursed() {
        BigDecimal sum = programRepository.sumTotalDisbursed();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Returns disbursements created within a date range.
     * Used by reporting-service for scoped report generation.
     */
    public List<Disbursement> getDisbursementsByDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        java.time.Instant from = start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant to   = end.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return disbursementRepository.findByCreatedAtBetween(from, to);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void verifyFarmerIsActive(UUID farmerId) {
        try {
            String statusStr = restTemplate.getForObject(
                    farmerServiceUrl + "/farmers/" + farmerId + "/status", String.class);
            if (statusStr != null) statusStr = statusStr.replace("\"", "").trim();
            if (!"Active".equalsIgnoreCase(statusStr)) {
                throw new IllegalStateException("Farmer is not Active.");
            }
        } catch (IllegalStateException e) {
            throw e; // propagate business rule violation
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify farmer status: " + e.getMessage(), e);
        }
    }

    private void sendNotification(UUID userId, String message) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "channel", "In_App",
                "content", message
        );
        try {
            restTemplate.postForObject(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception e) {
            System.err.println("[notification] Failed to notify user " + userId + ": " + e.getMessage());
        }
    }
}
