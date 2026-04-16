package com.agrichain.subsidy;

import com.agrichain.common.enums.SubsidyProgramStatus;
import com.agrichain.subsidy.dto.DisbursementRejectRequest;
import com.agrichain.subsidy.dto.DisbursementRequest;
import com.agrichain.subsidy.dto.ProgramRequest;
import com.agrichain.subsidy.entity.Disbursement;
import com.agrichain.subsidy.entity.SubsidyProgram;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/subsidies")
public class SubsidyController {

    private final SubsidyService subsidyService;

    public SubsidyController(SubsidyService subsidyService) {
        this.subsidyService = subsidyService;
    }

    // ── Programs ──────────────────────────────────────────────────────────────

    /**
     * POST /subsidies/programs
     * Program Manager creates a new program.
     * createdBy is extracted from the JWT — never trusted from the request body.
     */
    @PostMapping("/programs")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<UUID> createProgram(
            @Valid @RequestBody ProgramRequest request,
            Authentication auth) {
        // Override createdBy with the authenticated user's ID from JWT
        request.setCreatedBy(extractUserId(auth));
        UUID id = subsidyService.createProgram(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /subsidies/programs
     * Program Manager and Government Auditor can list programs.
     */
    @GetMapping("/programs")
    @PreAuthorize("hasAnyRole('PROGRAM_MANAGER','GOVERNMENT_AUDITOR')")
    public ResponseEntity<List<SubsidyProgram>> listPrograms() {
        return ResponseEntity.ok(subsidyService.listPrograms());
    }

    /**
     * GET /subsidies/programs/{id}
     */
    @GetMapping("/programs/{id}")
    @PreAuthorize("hasAnyRole('PROGRAM_MANAGER','GOVERNMENT_AUDITOR')")
    public ResponseEntity<SubsidyProgram> getProgram(@PathVariable UUID id) {
        return ResponseEntity.ok(subsidyService.getProgram(id));
    }

    /**
     * PUT /subsidies/programs/{id}/status?status=Active
     * Activate a Draft program.
     */
    @PutMapping("/programs/{id}/status")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<Void> updateProgramStatus(
            @PathVariable UUID id,
            @RequestParam SubsidyProgramStatus status) {
        subsidyService.updateProgramStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /subsidies/programs/{id}/close
     * Close an Active program.
     */
    @PutMapping("/programs/{id}/close")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<Void> closeProgram(@PathVariable UUID id) {
        subsidyService.updateProgramStatus(id, SubsidyProgramStatus.Closed);
        return ResponseEntity.noContent().build();
    }

    // ── Disbursements ─────────────────────────────────────────────────────────

    /**
     * POST /subsidies/disbursements
     * Program Manager creates a disbursement for a farmer.
     */
    @PostMapping("/disbursements")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<UUID> applyForDisbursement(
            @Valid @RequestBody DisbursementRequest request) {
        UUID id = subsidyService.applyForDisbursement(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /subsidies/disbursements
     * Program Manager and Government Auditor can list disbursements.
     */
    @GetMapping("/disbursements")
    @PreAuthorize("hasAnyRole('PROGRAM_MANAGER','GOVERNMENT_AUDITOR')")
    public ResponseEntity<List<Disbursement>> listDisbursements() {
        return ResponseEntity.ok(subsidyService.listDisbursements());
    }

    /**
     * PUT /subsidies/disbursements/{id}/approve
     * Approve a Pending disbursement.
     * reviewerId extracted from JWT — never random.
     */
    @PutMapping("/disbursements/{id}/approve")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<Void> approveDisbursement(
            @PathVariable UUID id,
            Authentication auth) {
        subsidyService.approveDisbursement(id, extractUserId(auth));
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /subsidies/disbursements/{id}/reject
     * Reject a Pending disbursement with a mandatory reason.
     * Reconciles the pre-allocated budget back to the program.
     */
    @PutMapping("/disbursements/{id}/reject")
    @PreAuthorize("hasRole('PROGRAM_MANAGER')")
    public ResponseEntity<Void> rejectDisbursement(
            @PathVariable UUID id,
            @Valid @RequestBody DisbursementRejectRequest request,
            Authentication auth) {
        subsidyService.rejectDisbursement(id, extractUserId(auth), request.getReason());
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /subsidies/total-disbursed
     * Internal — called by reporting-service. No auth required (permitted in SecurityConfig).
     */
    @GetMapping("/total-disbursed")
    public ResponseEntity<java.math.BigDecimal> getTotalDisbursed() {
        return ResponseEntity.ok(subsidyService.getTotalDisbursed());
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStateError(IllegalStateException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleServerError(RuntimeException ex) {
        System.err.println("[error] " + ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("error", "An internal error occurred."));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID extractUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isBlank()) {
            return UUID.fromString(s);
        }
        throw new IllegalStateException("Unable to extract userId from token");
    }
}
