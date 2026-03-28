package com.agrichain.farmer;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.farmer.dto.FarmerRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/farmers")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    /**
     * POST /farmers
     * Public registration endpoint.
     */
    @PostMapping
    public ResponseEntity<UUID> register(@Valid @RequestBody FarmerRegistrationRequest request) {
        UUID farmerId = farmerService.registerFarmer(request);
        return ResponseEntity.status(201).body(farmerId);
    }

    /**
     * PUT /farmers/{id}/status
     * Approved/Deactivates a farmer account.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusRequest) {
        com.agrichain.common.enums.FarmerStatus newStatus = com.agrichain.common.enums.FarmerStatus.valueOf(statusRequest.get(
                "status"));
        farmerService.updateStatus(id, newStatus);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /farmers/{id}/status
     * Returns the current status of the farmer.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<FarmerStatus> getFarmerStatus(@PathVariable UUID id) {
        return farmerService.getFarmer(id)
                .map(f -> ResponseEntity.ok(f.getStatus()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFarmerCount(@RequestParam(required = false) FarmerStatus status) {
        return ResponseEntity.ok(farmerService.countFarmers(status));
    }

    /**
     * GET /farmers/{id}
     * Retrieve farmer profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<com.agrichain.farmer.entity.Farmer> getFarmer(@PathVariable UUID id) {
        return farmerService.getFarmer(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /farmers/{id}
     * Update farmer profile with audit logging.
     */
    @PutMapping("/{id}")
    public ResponseEntity<com.agrichain.farmer.entity.Farmer> updateFarmer(@PathVariable UUID id, 
                                                                         @RequestBody com.agrichain.farmer.entity.Farmer request,
                                                                         @RequestHeader(value = "X-User-ID", required = false) String userId) {
        UUID currentUserId = (userId != null) ? UUID.fromString(userId) : id; 
        return ResponseEntity.ok(farmerService.updateFarmer(id, request, currentUserId));
    }

    /**
     * POST /farmers/{id}/documents
     * Document upload endpoint.
     */
    @PostMapping("/{id}/documents")
    public ResponseEntity<UUID> uploadDocument(@PathVariable UUID id, @RequestBody Map<String, String> docRequest) {
        com.agrichain.farmer.entity.DocumentType type = com.agrichain.farmer.entity.DocumentType.valueOf(docRequest.get(
                "type"));
        String path = docRequest.get("storagePath");
        UUID docId = farmerService.uploadDocument(id, type, path);
        return ResponseEntity.status(201).body(docId);
    }

    /**
     * PUT /farmers/{id}/documents/{docId}/verify
     * Document verification endpoint for Market Officers.
     */
    @PutMapping("/{id}/documents/{docId}/verify")
    public ResponseEntity<Void> verifyDocument(@PathVariable UUID id, 
                                               @PathVariable UUID docId,
                                               @RequestBody Map<String, String> verifyRequest,
                                               @RequestHeader(value = "X-User-ID", required = false) String officerId) {
        com.agrichain.common.enums.VerificationStatus status = com.agrichain.common.enums.VerificationStatus.valueOf(
                verifyRequest.get("status"));
        String reason = verifyRequest.get("reason");
        UUID currentOfficerId = (officerId != null) ? UUID.fromString(officerId) : null;
        
        farmerService.verifyDocument(id, docId, status, currentOfficerId, reason);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(IllegalArgumentException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleClientError(RuntimeException ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }
}
