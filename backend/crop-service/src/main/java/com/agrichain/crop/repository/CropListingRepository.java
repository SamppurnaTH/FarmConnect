package com.agrichain.crop.repository;

import com.agrichain.common.enums.ListingStatus;
import com.agrichain.crop.entity.CropListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CropListingRepository extends JpaRepository<CropListing, UUID>,
        JpaSpecificationExecutor<CropListing> {

    List<CropListing> findByStatus(ListingStatus status);
    List<CropListing> findByFarmerIdAndStatus(UUID farmerId, ListingStatus status);
    /** All listings for a farmer regardless of status — for the farmer's own view */
    List<CropListing> findByFarmerId(UUID farmerId);
    List<CropListing> findByStatusAndLocationContainingIgnoreCase(ListingStatus status, String location);

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(SUM(l.quantity), 0) FROM CropListing l WHERE l.status = 'Active'")
    java.math.BigDecimal sumActiveQuantity();
}
