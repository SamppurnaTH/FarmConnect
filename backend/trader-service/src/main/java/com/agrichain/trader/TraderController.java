package com.agrichain.trader;

import com.agrichain.trader.dto.TraderRegistrationRequest;
import com.agrichain.trader.entity.Trader;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/traders")
public class TraderController {

    private final TraderService traderService;

    public TraderController(TraderService traderService) {
        this.traderService = traderService;
    }

    /**
     * POST /traders
     * Public registration for Traders.
     */
    @PostMapping
    public ResponseEntity<UUID> register(@Valid @RequestBody TraderRegistrationRequest request) {
        UUID id = traderService.registerTrader(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /traders/{id}
     * Retrieve trader profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Trader> getTrader(@PathVariable UUID id) {
        return ResponseEntity.ok(traderService.getTrader(id));
    }

    /**
     * PUT /traders/{id}
     * Update trader profile.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Trader> updateTrader(@PathVariable UUID id, @RequestBody Trader request) {
        return ResponseEntity.ok(traderService.updateTrader(id, request));
    }

    /**
     * GET /traders/{id}/orders
     * Trader view own orders by querying Crops Service.
     */
    @GetMapping("/{id}/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrders(@PathVariable UUID id) {
        // We use the Trader's underlying user_id if needed, but for now we pass the trader's profile id
        // Actually, many services use user_id as the primary filter for orders.
        // I'll use the user_id associated with the trader profile.
        Trader trader = traderService.getTrader(id);
        return ResponseEntity.ok(traderService.getTraderOrders(trader.getUserId()));
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleServerError(RuntimeException ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }
}
