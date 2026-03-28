package com.agrichain.trader;

import com.agrichain.common.enums.UserStatus;
import com.agrichain.trader.dto.TraderRegistrationRequest;
import com.agrichain.trader.entity.Trader;
import com.agrichain.trader.repository.TraderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TraderService {

    private final TraderRepository traderRepository;
    private final RestTemplate restTemplate;

    @Value("${services.identity.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${services.crop.url:http://localhost:8083}")
    private String cropServiceUrl;

    public TraderService(TraderRepository traderRepository, RestTemplateBuilder restTemplateBuilder) {
        this.traderRepository = traderRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 11.1 (Implied): Trader registration.
     */
    @Transactional
    public UUID registerTrader(TraderRegistrationRequest request) {
        // 1. Check for duplicates
        if (traderRepository.existsByContactInfo(request.getContactInfo())) {
            throw new IllegalArgumentException("Contact info already registered.");
        }

        // 2. Create user in Identity Service
        Map<String, String> userRequest = Map.of(
            "username", request.getUsername(),
            "password", request.getPassword(),
            "email", request.getEmail(),
            "role", "Trader"
        );

        UUID userId;
        try {
            userId = restTemplate.postForObject(identityServiceUrl + "/auth/register", userRequest, UUID.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }

        // 3. Create Trader profile
        Trader trader = new Trader();
        trader.setUserId(userId);
        trader.setName(request.getName());
        trader.setOrganization(request.getOrganization());
        trader.setContactInfo(request.getContactInfo());
        trader.setStatus(UserStatus.Active);

        return traderRepository.save(trader).getId();
    }

    public Trader getTrader(UUID id) {
        return traderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trader not found with ID: " + id));
    }

    @Transactional
    public Trader updateTrader(UUID id, Trader request) {
        Trader existing = getTrader(id);
        existing.setName(request.getName());
        existing.setOrganization(request.getOrganization());
        // Note: Changing contact_info might require re-verification or uniqueness checks
        if (!existing.getContactInfo().equals(request.getContactInfo()) && 
            traderRepository.existsByContactInfo(request.getContactInfo())) {
            throw new IllegalArgumentException("New contact info already in use.");
        }
        existing.setContactInfo(request.getContactInfo());
        return traderRepository.save(existing);
    }

    /**
     * Task 7.5: Trader view own orders.
     * Calls Crop Service: GET /listings/orders (assuming such endpoint exists or using filtering)
     * Actually, Crop Service has GET /orders?traderId=...
     */
    public List<Map<String, Object>> getTraderOrders(UUID userId) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = restTemplate.getForObject(
                    cropServiceUrl + "/orders?traderId=" + userId, List.class);
            return orders;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch trader orders: " + e.getMessage(), e);
        }
    }
}
