package com.agrichain.trader.repository;

import com.agrichain.trader.entity.Trader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TraderRepository extends JpaRepository<Trader, UUID> {
    Optional<Trader> findByUserId(UUID userId);
    boolean existsByContactInfo(String contactInfo);
}
