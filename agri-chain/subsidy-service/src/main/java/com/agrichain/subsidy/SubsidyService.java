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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class SubsidyService {

    private final SubsidyProgramRepository programRepository;
    private final DisbursementRepository disbursementRepository;
    private final RestTemplate restTemplate;

    @Value("${services.farmer.url:http://localhost:8082}")
    private String farmerServiceUrl;

    public SubsidyService(SubsidyProgramRepository programRepository, 
                          DisbursementRepository disbursementRepository,
                          RestTemplateBuilder restTemplateBuilder) {
        this.programRepository = programRepository;
        this.disbursementRepository = disbursementRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

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
     * Requirement 11.2: Activate program. Monotonic transition.
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

    /**
     * Requirement 11.4: Create disbursement.
     * Enforces budget, status, and uniqueness.
     */
    @Transactional
    public UUID applyForDisbursement(DisbursementRequest request) {
        // 1. Check Program Status
        SubsidyProgram program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));

        if (program.getStatus() != SubsidyProgramStatus.Active) {
            throw new IllegalStateException("Disbursements only allowed for Active programs.");
        }

        // 2. Requirement 11.4: Check Remaining Budget
        BigDecimal remainingBudget = program.getBudgetAmount().subtract(program.getTotalDisbursed());
        if (request.getAmount().compareTo(remainingBudget) > 0) {
            throw new IllegalArgumentException("Disbursement exceeds program budget.");
        }

        // 3. Requirement 11.4: Verify Farmer is Active
        try {
            // Check for "Active" status from farmer service
            String statusStr = restTemplate.getForObject(
                    farmerServiceUrl + "/farmers/" + request.getFarmerId() + "/status", 
                    String.class);
            if (!"Active".equalsIgnoreCase(statusStr)) {
                throw new IllegalStateException("Farmer is not Active.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify farmer status: " + e.getMessage(), e);
        }

        // 4. Requirement 12.5: Unique (farmer, program, cycle)
        if (disbursementRepository.existsByFarmerIdAndProgramIdAndProgramCycle(
                request.getFarmerId(), request.getProgramId(), request.getProgramCycle())) {
            throw new IllegalStateException("Duplicate disbursement for this program cycle.");
        }

        // 5. Create Disbursement
        Disbursement disbursement = new Disbursement();
        disbursement.setProgramId(request.getProgramId());
        disbursement.setFarmerId(request.getFarmerId());
        disbursement.setAmount(request.getAmount());
        disbursement.setProgramCycle(request.getProgramCycle());
        disbursement.setStatus(DisbursementStatus.Pending);

        // Update total disbursed (pre-allocate)
        program.setTotalDisbursed(program.getTotalDisbursed().add(request.getAmount()));
        programRepository.save(program);

        return disbursementRepository.save(disbursement).getId();
    }

    /**
     * Requirement 12.2: Approve disbursement.
     */
    @Transactional
    public void approveDisbursement(UUID id, UUID reviewerId) {
        Disbursement disbursement = disbursementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found"));

        if (disbursement.getStatus() != DisbursementStatus.Pending) {
            throw new IllegalStateException("Disbursement is already processed.");
        }

        disbursement.setStatus(DisbursementStatus.Approved);
        disbursement.setApprovedBy(reviewerId);
        disbursement.setApprovedAt(Instant.now());

        disbursementRepository.save(disbursement);
    }

    public java.math.BigDecimal getTotalDisbursed() {
        return programRepository.findAll().stream()
                .map(SubsidyProgram::getTotalDisbursed)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
