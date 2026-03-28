package com.agrichain.subsidy;

import com.agrichain.common.enums.SubsidyProgramStatus;
import com.agrichain.subsidy.dto.DisbursementRequest;
import com.agrichain.subsidy.dto.ProgramRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/subsidies")
public class SubsidyController {

    private final SubsidyService subsidyService;

    public SubsidyController(SubsidyService subsidyService) {
        this.subsidyService = subsidyService;
    }

    /**
     * POST /subsidies/programs
     * Create a new subsidy program (Draft).
     */
    @PostMapping("/programs")
    public ResponseEntity<UUID> createProgram(@Valid @RequestBody ProgramRequest request) {
        UUID id = subsidyService.createProgram(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * PUT /subsidies/programs/{id}/status
     * Activate or Close a program. Monotonic transition.
     */
    @PutMapping("/programs/{id}/status")
    public ResponseEntity<Void> updateProgramStatus(@PathVariable UUID id, @RequestParam SubsidyProgramStatus status) {
        subsidyService.updateProgramStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /subsidies/disbursements
     * Farmer apply for disbursement.
     */
    @PostMapping("/disbursements")
    public ResponseEntity<UUID> applyForDisbursement(@Valid @RequestBody DisbursementRequest request) {
        UUID id = subsidyService.applyForDisbursement(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * PUT /subsidies/disbursements/{id}/approve
     * Market Officer approve disbursement.
     */
    @PutMapping("/disbursements/{id}/approve")
    public ResponseEntity<Void> approveDisbursement(@PathVariable UUID id, @RequestParam UUID reviewerId) {
        subsidyService.approveDisbursement(id, reviewerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/total-disbursed")
    public ResponseEntity<java.math.BigDecimal> getTotalDisbursed() {
        return ResponseEntity.ok(subsidyService.getTotalDisbursed());
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleServerError(RuntimeException ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }
}
