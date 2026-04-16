package com.agrichain.trader;

import com.agrichain.common.enums.UserStatus;
import com.agrichain.trader.dto.TraderProfileResponse;
import com.agrichain.trader.dto.TraderRegistrationRequest;
import com.agrichain.trader.dto.UpdateTraderRequest;
import com.agrichain.trader.entity.Trader;
import com.agrichain.trader.repository.TraderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class TraderService {

    private final TraderRepository traderRepository;
    private final RestTemplate restTemplate;

    @Value("${services.identity.url:http://localhost:8081}")
    private String identityServiceUrl;

    @Value("${services.notification.url:http://localhost:8088}")
    private String notificationServiceUrl;

    public TraderService(TraderRepository traderRepository,
                         RestTemplateBuilder restTemplateBuilder) {
        this.traderRepository = traderRepository;
        this.restTemplate     = restTemplateBuilder.build();
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @Transactional
    public UUID registerTrader(TraderRegistrationRequest request) {
        if (traderRepository.existsByContactInfo(request.getContactInfo())) {
            throw new IllegalArgumentException("Contact info already registered.");
        }

        Map<String, String> userRequest = Map.of(
                "username", request.getUsername(),
                "password", request.getPassword(),
                "email",    request.getEmail(),
                "role",     "TRADER"
        );

        UUID userId;
        try {
            userId = restTemplate.postForObject(
                    identityServiceUrl + "/auth/register", userRequest, UUID.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("Username or email already registered.");
            }
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }

        Trader trader = new Trader();
        trader.setUserId(userId);
        trader.setName(request.getName());
        trader.setOrganization(request.getOrganization());
        trader.setContactInfo(request.getContactInfo());
        trader.setStatus(UserStatus.Active);

        return traderRepository.save(trader).getId();
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    public TraderProfileResponse getTraderById(UUID id) {
        return traderRepository.findById(id)
                .map(TraderProfileResponse::from)
                .orElseThrow(() -> new TraderNotFoundException("Trader not found: " + id));
    }

    /**
     * Lookup by identity userId — used by the frontend after login.
     * JWT carries userId (identity ID), not the trader profile ID.
     */
    public Optional<TraderProfileResponse> getTraderByUserId(UUID userId) {
        return traderRepository.findByUserId(userId).map(TraderProfileResponse::from);
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Transactional
    public TraderProfileResponse updateTrader(UUID traderId, UpdateTraderRequest request) {
        Trader existing = traderRepository.findById(traderId)
                .orElseThrow(() -> new TraderNotFoundException("Trader not found: " + traderId));

        if (request.getName() != null)        existing.setName(request.getName());
        if (request.getOrganization() != null) existing.setOrganization(request.getOrganization());
        if (request.getContactInfo() != null) {
            if (!existing.getContactInfo().equals(request.getContactInfo()) &&
                    traderRepository.existsByContactInfo(request.getContactInfo())) {
                throw new IllegalArgumentException("Contact info already in use.");
            }
            existing.setContactInfo(request.getContactInfo());
        }

        return TraderProfileResponse.from(traderRepository.save(existing));
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    /**
     * Sends an In_App notification to the trader when their order status changes.
     * Called by crop-service indirectly — but here we expose it for direct use.
     */
    public void sendNotification(UUID userId, String message) {
        Map<String, Object> request = Map.of(
                "userId",  userId,
                "channel", "In_App",
                "content", message
        );
        try {
            restTemplate.postForObject(notificationServiceUrl + "/notifications", request, Void.class);
        } catch (Exception e) {
            System.err.println("[notification] Failed to send to trader " + userId + ": " + e.getMessage());
        }
    }
}
