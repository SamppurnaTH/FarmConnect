package com.agrichain.trader;

import com.agrichain.trader.dto.TraderProfileResponse;
import com.agrichain.trader.dto.TraderRegistrationRequest;
import com.agrichain.trader.dto.UpdateTraderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/traders")
public class TraderController {

    private final TraderService traderService;

    public TraderController(TraderService traderService) {
        this.traderService = traderService;
    }

    // ── Registration (public) ─────────────────────────────────────────────────

    /**
     * POST /traders
     * Public self-registration for traders.
     */
    @PostMapping
    public ResponseEntity<UUID> register(@Valid @RequestBody TraderRegistrationRequest request) {
        UUID id = traderService.registerTrader(request);
        return ResponseEntity.status(201).body(id);
    }

    // ── Profile reads ─────────────────────────────────────────────────────────

    /**
     * GET /traders/me
     * Returns the trader profile for the currently authenticated user.
     * JWT carries userId (identity ID); we look up the trader by that userId.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<TraderProfileResponse> getMyProfile(Authentication auth) {
        UUID userId = extractUserId(auth);
        return traderService.getTraderByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /traders/{id}
     * Retrieve a trader profile by trader ID.
     * Trader can view their own; Administrators can view any.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRADER','ADMINISTRATOR','MARKET_OFFICER')")
    public ResponseEntity<TraderProfileResponse> getTrader(@PathVariable UUID id) {
        return ResponseEntity.ok(traderService.getTraderById(id));
    }

    // ── Profile updates ───────────────────────────────────────────────────────

    /**
     * PUT /traders/{id}
     * Update own profile. Trader can only update their own record.
     * userId extracted from JWT — never trusted from a header.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<TraderProfileResponse> updateTrader(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTraderRequest request,
            Authentication auth) {

        // Ownership check: trader can only update their own profile
        UUID requestingUserId = extractUserId(auth);
        traderService.getTraderByUserId(requestingUserId)
                .filter(p -> p.getId().equals(id))
                .orElseThrow(() -> new SecurityException("You can only update your own profile."));

        return ResponseEntity.ok(traderService.updateTrader(id, request));
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(TraderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(TraderNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(SecurityException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
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
