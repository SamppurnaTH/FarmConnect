package com.agrichain.transaction;

import com.agrichain.transaction.dto.PaymentRequest;
import com.agrichain.transaction.dto.TransactionRequest;
import com.agrichain.transaction.entity.Transaction;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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
     * Internal endpoint for Crop Service on order confirmation.
     */
    @PostMapping
    public ResponseEntity<UUID> create(@Valid @RequestBody TransactionRequest request) {
        UUID id = transactionService.createTransaction(request);
        return ResponseEntity.status(201).body(id);
    }

    /**
     * POST /transactions/{id}/payments
     * Trader submits payment.
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<UUID> processPayment(@PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        // Ensure transactionId in body matches path if needed
        request.setTransactionId(id);
        UUID paymentId = transactionService.processPayment(request);
        return ResponseEntity.status(201).body(paymentId);
    }

    @GetMapping("/total-value")
    public ResponseEntity<java.math.BigDecimal> getTotalValue() {
        return ResponseEntity.ok(transactionService.getTotalSettledValue());
    }

    @GetMapping
    public ResponseEntity<java.util.List<Transaction>> listTransactions() {
        return ResponseEntity.ok(transactionService.listTransactions());
    }

    /**
     * GET /transactions/{id}
     * Retrieve transaction status.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransaction(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
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
