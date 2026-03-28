package com.agrichain.crop;

import com.agrichain.crop.dto.OrderRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
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
     * Buyer places an order.
     */
    @PostMapping
    public ResponseEntity<UUID> placeOrder(@Valid @RequestBody OrderRequest request) {
        UUID id = orderService.placeOrder(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /orders
     * Fetch orders by traderId or listingId.
     */
    @GetMapping
    public ResponseEntity<List<com.agrichain.crop.entity.Order>> getOrders(
            @RequestParam(required = false) UUID traderId,
            @RequestParam(required = false) UUID listingId) {
        if (listingId != null) {
            return ResponseEntity.ok(orderService.getOrdersByListing(listingId));
        }
        if (traderId != null) {
            return ResponseEntity.ok(orderService.getTraderOrders(traderId));
        }
        return ResponseEntity.ok(java.util.Collections.emptyList());
    }

    /**
     * PUT /orders/{id}/status
     * Farmer accepts or rejects order.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> statusRequest) {
        com.agrichain.common.enums.OrderStatus status = com.agrichain.common.enums.OrderStatus.valueOf(
                statusRequest.get("status"));
        orderService.updateOrderStatus(id, status);
        return ResponseEntity.noContent().build();
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
}
