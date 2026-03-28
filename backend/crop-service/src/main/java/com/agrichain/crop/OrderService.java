package com.agrichain.crop;

import com.agrichain.common.enums.OrderStatus;
import com.agrichain.common.enums.ListingStatus;
import com.agrichain.crop.dto.OrderRequest;
import com.agrichain.crop.entity.CropListing;
import com.agrichain.crop.entity.Order;
import com.agrichain.crop.repository.CropListingRepository;
import com.agrichain.crop.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CropListingRepository cropListingRepository;
    private final RestTemplate restTemplate;

    @Value("${services.transaction.url:http://localhost:8085}")
    private String transactionServiceUrl;

    public OrderService(OrderRepository orderRepository, 
                        CropListingRepository cropListingRepository,
                        RestTemplateBuilder restTemplateBuilder) {
        this.orderRepository = orderRepository;
        this.cropListingRepository = cropListingRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 9.1: Buyer places an order.
     */
    @Transactional
    public UUID placeOrder(OrderRequest request) {
        CropListing listing = cropListingRepository.findById(request.getListingId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

        if (listing.getStatus() != ListingStatus.Active) {
            throw new IllegalStateException("Listing is not active.");
        }

        if (listing.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("Insufficient quantity available.");
        }

        Order order = new Order();
        order.setListingId(request.getListingId());
        order.setTraderId(request.getBuyerId());
        order.setQuantity(request.getQuantity());
        order.setStatus(OrderStatus.Pending);

        return orderRepository.save(order).getId();
    }

    /**
     * Requirement 9.3: Farmer accepts or rejects order.
     * Deducts quantity only upon confirmation.
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.Pending) {
            throw new IllegalStateException("Order is already processed.");
        }

        if (newStatus == OrderStatus.Confirmed) {
            CropListing listing = cropListingRepository.findById(order.getListingId())
                    .orElseThrow(() -> new IllegalStateException("Listing no longer exists"));
            
            if (listing.getQuantity().compareTo(order.getQuantity()) < 0) {
                throw new IllegalStateException("Insufficient quantity to confirm order.");
            }

            // Deduct quantity
            listing.setQuantity(listing.getQuantity().subtract(order.getQuantity()));
            if (listing.getQuantity().compareTo(java.math.BigDecimal.ZERO) == 0) {
                listing.setStatus(ListingStatus.Closed);
            }
            cropListingRepository.save(listing);

            // Requirement 9.1: Confirmed order creates a Transaction
            createTransaction(order, listing.getPricePerUnit());
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    private void createTransaction(Order order, java.math.BigDecimal pricePerUnit) {
        java.math.BigDecimal amount = order.getQuantity().multiply(pricePerUnit);
        Map<String, Object> transactionRequest = Map.of(
            "orderId", order.getId(),
            "amount", amount
        );

        try {
            restTemplate.postForObject(transactionServiceUrl + "/transactions", transactionRequest, UUID.class);
        } catch (Exception e) {
            // Requirement: If transaction service fails, we might want to log and handle it.
            // For now, we log and proceed or throw depending on policy.
            throw new RuntimeException("Failed to create transaction for order: " + e.getMessage(), e);
        }
    }

    public java.util.List<Order> getTraderOrders(UUID traderId) {
        return orderRepository.findByTraderId(traderId);
    }
}
