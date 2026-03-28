package com.agrichain.farmer.repository;

import com.agrichain.farmer.entity.FarmerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FarmerDocumentRepository extends JpaRepository<FarmerDocument, UUID> {
    List<FarmerDocument> findByFarmerId(UUID farmerId);
}
