package com.agrichain.transaction;

import com.agrichain.common.enums.PaymentStatus;
import com.agrichain.common.enums.TransactionStatus;
import com.agrichain.transaction.dto.PaymentRequest;
import com.agrichain.transaction.dto.TransactionRequest;
import com.agrichain.transaction.entity.Payment;
import com.agrichain.transaction.entity.Transaction;
import com.agrichain.transaction.repository.PaymentRepository;
import com.agrichain.transaction.repository.TransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              PaymentRepository paymentRepository) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
    }

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Requirement 9.1: Confirmed order creates a Transaction.
     * Called internally by crop-service — no user JWT.
     */
    @Transactional
    public UUID createTransaction(TransactionRequest request) {
        if (transactionRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalStateException("Transaction already exists for order: " + request.getOrderId());
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(request.getOrderId());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.Pending_Payment);

        return transactionRepository.save(transaction).getId();
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    /**
     * Requirement 10.1: Trader submits payment.
     */
    @Transactional
    public UUID processPayment(PaymentRequest request) {
        Transaction transaction = transactionRepository.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getStatus() != TransactionStatus.Pending_Payment) {
            throw new IllegalStateException("Transaction is not in a payable state: " + transaction.getStatus());
        }

        if (transaction.getExpiresAt().isBefore(Instant.now())) {
            transaction.setStatus(TransactionStatus.Cancelled);
            transactionRepository.save(transaction);
            throw new IllegalStateException("Transaction has expired.");
        }

        Payment payment = new Payment();
        payment.setTransactionId(request.getTransactionId());
        payment.setMethod(request.getMethod());
        payment.setGatewayRef(request.getGatewayRef());
        payment.setStatus(PaymentStatus.Completed); // Mock gateway success

        transaction.setStatus(TransactionStatus.Settled);
        transactionRepository.save(transaction);

        return paymentRepository.save(payment).getId();
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    public Transaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    public List<Transaction> listTransactions() {
        return transactionRepository.findAll();
    }

    /**
     * Returns transactions whose orderId is in the provided set.
     * Used to filter transactions for a specific farmer or trader.
     */
    public List<Transaction> getTransactionsByOrderIds(Collection<UUID> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return List.of();
        return transactionRepository.findByOrderIdIn(orderIds);
    }

    /**
     * Uses a SUM aggregate query — does NOT load all rows into memory.
     */
    public BigDecimal getTotalSettledValue() {
        BigDecimal sum = transactionRepository.sumSettledAmount();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    /**
     * Returns transactions created within a date range.
     * Used by reporting-service for scoped report generation.
     */
    public List<Transaction> getTransactionsByDateRange(java.time.LocalDate start, java.time.LocalDate end) {
        java.time.Instant from = start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        java.time.Instant to   = end.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        return transactionRepository.findByCreatedAtBetween(from, to);
    }

    // ── Scheduler ─────────────────────────────────────────────────────────────

    /**
     * Requirement 10.5: Auto-cancel transactions that have passed their 48h expiry.
     * Runs every hour.
     */
    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    public void cancelExpiredTransactions() {
        List<Transaction> expired = transactionRepository.findByStatusAndExpiresAtBefore(
                TransactionStatus.Pending_Payment, Instant.now());

        for (Transaction t : expired) {
            t.setStatus(TransactionStatus.Cancelled);
            transactionRepository.save(t);
        }

        if (!expired.isEmpty()) {
            System.out.println("[scheduler] Cancelled " + expired.size() + " expired transactions.");
        }
    }
}
