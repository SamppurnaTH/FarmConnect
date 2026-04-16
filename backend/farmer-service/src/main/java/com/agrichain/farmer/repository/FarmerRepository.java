package com.agrichain.farmer.repository;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.farmer.entity.Farmer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    /** COUNT query — does not load rows into memory. */
    long countByStatus(FarmerStatus status);

    /**
     * Paginated list filtered by status.
     * Used by Market Officer farmer management with pagination.
     */
    Page<Farmer> findByStatus(FarmerStatus status, Pageable pageable);

    /**
     * Paginated search by status + name (partial, case-insensitive).
     * name column is AES-encrypted so exact-match search is not possible at DB level.
     * This query does a full-table scan on the status subset — acceptable for moderate data volumes.
     * For large scale, switch to a search index or deterministic encryption.
     */
    @Query("SELECT f FROM Farmer f WHERE f.status = :status")
    Page<Farmer> findByStatusPaged(@Param("status") FarmerStatus status, Pageable pageable);

    /** All farmers paginated (no status filter) */
    Page<Farmer> findAll(Pageable pageable);

    /**
     * Returns farmers registered within a date range.
     * Used by reporting-service for scoped report generation.
     */
    List<Farmer> findByCreatedAtBetween(java.time.Instant start, java.time.Instant end);
}
