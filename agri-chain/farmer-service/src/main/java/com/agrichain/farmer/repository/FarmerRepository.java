package com.agrichain.farmer.repository;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.farmer.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, UUID> {
    List<Farmer> findByStatus(com.agrichain.common.enums.FarmerStatus status);
    Optional<Farmer> findByUserId(UUID userId);
    boolean existsByContactInfoAndStatus(String contactInfo, FarmerStatus status);
    boolean existsByContactInfoAndStatusIn(String contactInfo, java.util.Collection<FarmerStatus> statuses);
}
