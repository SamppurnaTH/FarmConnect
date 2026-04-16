package com.agrichain.crop.repository;

import com.agrichain.common.enums.OrderStatus;
import com.agrichain.crop.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByListingId(UUID listingId);
    List<Order> findByTraderId(UUID traderId);
    List<Order> findByTraderIdAndStatus(UUID traderId, OrderStatus status);

    /** All orders for listings owned by a given farmer — used by FarmerOrdersPage */
    @org.springframework.data.jpa.repository.Query(
        "SELECT o FROM Order o JOIN CropListing l ON o.listingId = l.id WHERE l.farmerId = :farmerId")
    List<Order> findByFarmerId(@org.springframework.data.repository.query.Param("farmerId") UUID farmerId);
}
