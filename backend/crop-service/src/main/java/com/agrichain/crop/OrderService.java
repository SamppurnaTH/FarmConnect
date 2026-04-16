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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CropListingRepository cropListingRepository;
    private final RestTemplate restTemplate;

    @Value("${services.transaction.url:http://localhost:8084}")
    private String transactionServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

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
     * Enforces ownership — only the farmer who owns the listing can act on its orders.
     *
     * @param orderId       the order to update
     * @param newStatus     Confirmed or Cancelled
     * @param requestingFarmerId the farmer making the request (from JWT)
     */
    @Transactional
    public void updateOrderStatus(UUID orderId, OrderStatus newStatus, UUID requestingFarmerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.Pending) {
            throw new IllegalStateException("Order is already processed.");
        }

        CropListing listing = cropListingRepository.findById(order.getListingId())
                .orElseThrow(() -> new IllegalStateException("Listing no longer exists"));

        // Ownership check — farmer can only act on orders for their own listings
        if (!listing.getFarmerId().equals(requestingFarmerId)) {
            throw new SecurityException("You are not authorised to update this order.");
        }

        if (newStatus == OrderStatus.Confirmed) {
            if (listing.getQuantity().compareTo(order.getQuantity()) < 0) {
                throw new IllegalStateException("Insufficient quantity to confirm order.");
            }

            // Deduct quantity
            listing.setQuantity(listing.getQuantity().subtract(order.getQuantity()));
            if (listing.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                listing.setStatus(ListingStatus.Closed);
            }
            cropListingRepository.save(listing);

            // Requirement 9.1: Confirmed order creates a Transaction
            createTransaction(order, listing.getPricePerUnit());
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        // Notify the trader of the outcome
        notifyTrader(order, newStatus, listing.getCropType());
    }

    /**
     * Trader cancels their own pending order.
     * Only allowed while order is still Pending.
     *
     * @param orderId           the order to cancel
     * @param requestingTraderId the trader making the request (from JWT)
     */
    @Transactional
    public void cancelOrderByTrader(UUID orderId, UUID requestingTraderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        if (order.getStatus() != OrderStatus.Pending) {
            throw new IllegalStateException("Only pending orders can be cancelled.");
        }

        // Ownership check — trader can only cancel their own orders
        if (!order.getTraderId().equals(requestingTraderId)) {
            throw new SecurityException("You are not authorised to cancel this order.");
        }

        order.setStatus(OrderStatus.Cancelled);
        orderRepository.save(order);
    }

    public List<Order> getTraderOrders(UUID traderId) {
        return orderRepository.findByTraderId(traderId);
    }

    public List<Order> getOrdersByListing(UUID listingId) {
        return orderRepository.findByListingId(listingId);
    }

    /**
     * Returns all orders placed against listings owned by the given farmer.
     */
    public List<Order> getFarmerOrders(UUID farmerId) {
        return orderRepository.findByFarmerId(farmerId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void notifyTrader(Order order, OrderStatus newStatus, String cropType) {
        String message = switch (newStatus) {
            case Confirmed -> "Your order for '" + cropType + "' (" + order.getQuantity()
                    + " kg) has been confirmed. Please complete payment within 48 hours.";
            case Declined  -> "Your order for '" + cropType + "' (" + order.getQuantity()
                    + " kg) was declined by the farmer.";
            case Cancelled -> "Your order for '" + cropType + "' has been cancelled.";
            default -> "Your order status has been updated to " + newStatus.name();
        };
        sendNotification(order.getTraderId(), message);
    }

    private void sendNotification(UUID userId, String message) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "channel", "In_App",
                "content", message
        );
        try {
            restTemplate.postForObject(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception e) {
            System.err.println("[notification] Failed to notify trader " + userId + ": " + e.getMessage());
        }
    }

    private void createTransaction(Order order, BigDecimal pricePerUnit) {
        BigDecimal amount = order.getQuantity().multiply(pricePerUnit);
        Map<String, Object> transactionRequest = Map.of(
                "orderId", order.getId(),
                "amount",  amount
        );
        try {
            restTemplate.postForObject(transactionServiceUrl + "/transactions", transactionRequest, UUID.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create transaction for order: " + e.getMessage(), e);
        }
    }
}
