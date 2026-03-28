package com.agrichain.transaction;

import com.agrichain.common.enums.PaymentStatus;
import com.agrichain.common.enums.TransactionStatus;
import com.agrichain.transaction.dto.PaymentRequest;
import com.agrichain.transaction.dto.TransactionRequest;
import com.agrichain.transaction.entity.Payment;
import com.agrichain.transaction.entity.Transaction;
import com.agrichain.transaction.repository.PaymentRepository;
import com.agrichain.transaction.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    public TransactionService(TransactionRepository transactionRepository, PaymentRepository paymentRepository) {
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
    }

    /**
     * Requirement 9.1: Confirmed order creates a Transaction.
     */
    @Transactional
    public UUID createTransaction(TransactionRequest request) {
        // Enforce uniqueness of transaction per order
        if (transactionRepository.findByOrderId(request.getOrderId()).isPresent()) {
            throw new IllegalStateException("Transaction already exists for order: " + request.getOrderId());
        }

        Transaction transaction = new Transaction();
        transaction.setOrderId(request.getOrderId());
        transaction.setAmount(request.getAmount());
        transaction.setStatus(TransactionStatus.Pending_Payment);
        
        return transactionRepository.save(transaction).getId();
    }

    /**
     * Requirement 10.1: Trader submits payment.
     * Mocking gateway success for now.
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
        payment.setStatus(PaymentStatus.Completed); // Mock success

        // Requirement 10.2: Gateway success settles the transaction
        transaction.setStatus(TransactionStatus.Settled);
        transactionRepository.save(transaction);

        return paymentRepository.save(payment).getId();
    }

    public Transaction getTransaction(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    /**
     * Requirement 10.5: Transaction auto-cancellation after 48h.
     */
    @Transactional
    public void cancelExpiredTransactions() {
        List<Transaction> expired = transactionRepository.findByStatusAndExpiresAtBefore(
                TransactionStatus.Pending_Payment, Instant.now());
        
        for (Transaction t : expired) {
            t.setStatus(TransactionStatus.Cancelled);
            transactionRepository.save(t);
        }
    }

    public java.math.BigDecimal getTotalSettledValue() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getStatus() == TransactionStatus.Settled)
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public List<Transaction> listTransactions() {
        return transactionRepository.findAll();
    }
}
