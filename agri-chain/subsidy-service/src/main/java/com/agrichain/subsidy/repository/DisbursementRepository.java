package com.agrichain.subsidy.repository;

import com.agrichain.subsidy.entity.Disbursement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisbursementRepository extends JpaRepository<Disbursement, UUID> {
    
    /**
     * Requirement 12.5: Ensure unique (farmer_id, program_id, program_cycle).
     */
    boolean existsByFarmerIdAndProgramIdAndProgramCycle(UUID farmerId, UUID programId, String programCycle);
}
