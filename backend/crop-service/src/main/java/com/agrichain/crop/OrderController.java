package com.agrichain.crop;

import com.agrichain.common.enums.OrderStatus;
import com.agrichain.crop.dto.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /orders
     * Trader places an order on an active listing.
     */
    @PostMapping
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<UUID> placeOrder(@Valid @RequestBody OrderRequest request) {
        UUID id = orderService.placeOrder(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /orders
     * - ?farmerId=  → orders on that farmer's listings
     * - ?traderId=  → orders placed by that trader (enforces ownership: trader can only see own)
     * - ?listingId= → orders for a specific listing
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('FARMER','TRADER','MARKET_OFFICER','ADMINISTRATOR')")
    public ResponseEntity<List<com.agrichain.crop.entity.Order>> getOrders(
            @RequestParam(required = false) UUID farmerId,
            @RequestParam(required = false) UUID traderId,
            @RequestParam(required = false) UUID listingId,
            Authentication auth) {

        if (farmerId != null) {
            return ResponseEntity.ok(orderService.getFarmerOrders(farmerId));
        }
        if (listingId != null) {
            return ResponseEntity.ok(orderService.getOrdersByListing(listingId));
        }
        if (traderId != null) {
            // Ownership: Trader role can only query their own orders
            String role = auth.getAuthorities().iterator().next().getAuthority();
            if (role.equals("ROLE_TRADER")) {
                UUID requestingUserId = extractUserId(auth);
                if (!traderId.equals(requestingUserId)) {
                    return ResponseEntity.status(403).build();
                }
            }
            return ResponseEntity.ok(orderService.getTraderOrders(traderId));
        }
        return ResponseEntity.ok(List.of());
    }

    /**
     * PUT /orders/{id}/status
     * Farmer accepts (Confirmed) or declines (Declined) a pending order.
     * Ownership is enforced in the service layer using the JWT userId.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<Void> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        OrderStatus status = parseOrderStatus(body.get("status"));
        UUID farmerId = extractUserId(auth);
        orderService.updateOrderStatus(id, status, farmerId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /orders/{id}
     * Trader cancels their own pending order.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            Authentication auth) {

        UUID traderId = extractUserId(auth);
        orderService.cancelOrderByTrader(id, traderId);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────────

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(SecurityException ex) {
        return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleStateError(IllegalStateException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID extractUserId(Authentication auth) {
        Object details = auth.getDetails();
        if (details instanceof String s && !s.isBlank()) {
            return UUID.fromString(s);
        }
        throw new IllegalStateException("Unable to extract userId from token");
    }

    private OrderStatus parseOrderStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + value);
        }
    }
}
