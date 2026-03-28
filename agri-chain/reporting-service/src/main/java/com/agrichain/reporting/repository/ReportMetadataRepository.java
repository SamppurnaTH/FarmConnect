package com.agrichain.reporting.repository;

import com.agrichain.reporting.entity.ReportMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportMetadataRepository extends JpaRepository<ReportMetadata, UUID> {
}
