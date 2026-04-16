package com.agrichain.transaction.repository;

import com.agrichain.common.enums.TransactionStatus;
import com.agrichain.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<Transaction> findByOrderId(UUID orderId);
    List<Transaction> findByStatusAndExpiresAtBefore(TransactionStatus status, Instant now);
    List<Transaction> findByOrderIdIn(java.util.Collection<UUID> orderIds);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = 'Settled'")
    java.math.BigDecimal sumSettledAmount();
}
