package com.agrichain.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Background task to collect and cancel expired transactions.
 * Requirement 10.5: Pending payment is auto-cancelled after 48 hours.
 */
@Component
public class TransactionExpiryCollector {

    private static final Logger log = LoggerFactory.getLogger(TransactionExpiryCollector.class);
    private final TransactionService transactionService;

    public TransactionExpiryCollector(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Run every 10 minutes to check for expired transactions.
     */
    @Scheduled(fixedDelay = 600000)
    public void collectExpired() {
        log.info("Starting scheduled cleanup of expired transactions.");
        try {
            transactionService.cancelExpiredTransactions();
            log.info("Finished cleanup of expired transactions.");
        } catch (Exception e) {
            log.error("Failed to cleanup expired transactions: {}", e.getMessage(), e);
        }
    }
}
