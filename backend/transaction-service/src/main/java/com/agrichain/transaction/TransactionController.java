package com.agrichain.transaction;

import com.agrichain.transaction.dto.PaymentRequest;
import com.agrichain.transaction.dto.TransactionRequest;
import com.agrichain.transaction.entity.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * POST /transactions
     * Internal — called by crop-service on order confirmation. No user JWT.
     */
    @PostMapping
    public ResponseEntity<UUID> create(@Valid @RequestBody TransactionRequest request) {
        UUID id = transactionService.createTransaction(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * GET /transactions
     * - With ?orderIds= → returns transactions for those specific orders.
     *   Used by Farmer and Trader to see only their own transactions.
     *   The frontend is responsible for passing only their own orderIds.
     * - Without ?orderIds= → returns all transactions.
     *   Restricted to Market Officers, Admins, Compliance Officers, Auditors.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Transaction>> listTransactions(
            @RequestParam(required = false) Set<UUID> orderIds,
            Authentication auth) {

        if (orderIds != null && !orderIds.isEmpty()) {
            // Any authenticated user can filter by specific orderIds (they should only pass their own)
            return ResponseEntity.ok(transactionService.getTransactionsByOrderIds(orderIds));
        }

        // Full list — restricted to privileged roles
        String role = auth.getAuthorities().iterator().next().getAuthority();
        boolean privileged = role.equals("ROLE_MARKET_OFFICER") || role.equals("ROLE_ADMINISTRATOR")
                || role.equals("ROLE_COMPLIANCE_OFFICER") || role.equals("ROLE_GOVERNMENT_AUDITOR");
        if (!privileged) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(transactionService.listTransactions());
    }

    /**
     * GET /transactions/{id}
     * Retrieve a single transaction. Accessible by authenticated users.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    /**
     * POST /transactions/{id}/payments
     * Trader submits payment for a pending transaction.
     */
    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAnyRole('TRADER','FARMER')")
    public ResponseEntity<UUID> processPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request) {
        request.setTransactionId(id);
        UUID paymentId = transactionService.processPayment(request);
        return ResponseEntity.status(201).body(paymentId);
    }

    /**
     * GET /transactions/total-value
     * Internal — called by reporting-service. No auth required.
     */
    @GetMapping("/total-value")
    public ResponseEntity<java.math.BigDecimal> getTotalValue() {
        return ResponseEntity.ok(transactionService.getTotalSettledValue());
    }

    /**
     * GET /transactions/report?start=&end=
     * Internal — called by reporting-service to fetch transactions in a date range.
     * No auth required (service-to-service).
     */
    @GetMapping("/report")
    public ResponseEntity<List<Transaction>> getTransactionsForReport(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        return ResponseEntity.ok(transactionService.getTransactionsByDateRange(start, end));
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
}
