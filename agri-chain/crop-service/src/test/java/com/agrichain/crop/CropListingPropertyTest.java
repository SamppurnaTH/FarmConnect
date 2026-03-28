package com.agrichain.crop;

import com.agrichain.common.enums.ListingStatus;
import com.agrichain.crop.dto.ListingRequest;
import com.agrichain.crop.entity.CropListing;
import com.agrichain.crop.repository.CropListingRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CropListingPropertyTest {

    private final CropListingRepository repository = Mockito.mock(CropListingRepository.class);
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final CropListingService service;

    public CropListingPropertyTest() {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        this.service = new CropListingService(repository, builder);
    }

    /**
     * Property 11: Valid crop listing creation requires an Active farmer status.
     */
    @Property
    void activeFarmerCanCreateListing(@ForAll("validListingRequests") ListingRequest request) {
        // Setup mock: Farmer is active
        when(restTemplate.getForObject(any(String.class), eq(Map.class)))
                .thenReturn(Map.of("status", "Active"));
        
        when(repository.save(any(CropListing.class))).thenAnswer(invocation -> {
            CropListing l = invocation.getArgument(0);
            l.setId(UUID.randomUUID());
            return l;
        });

        UUID id = service.createListing(request);
        Assertions.assertNotNull(id);
    }

    /**
     * Property 11 (Failure): Inactive farmer cannot create listing.
     */
    @Property
    void inactiveFarmerCannotCreateListing(@ForAll("validListingRequests") ListingRequest request, 
                                           @ForAll("inactiveStatuses") String status) {
        // Setup mock: Farmer is NOT active
        when(restTemplate.getForObject(any(String.class), eq(Map.class)))
                .thenReturn(Map.of("status", status));

        try {
            service.createListing(request);
            Assertions.fail("Expected an exception to be thrown for inactive farmer");
        } catch (RuntimeException e) {
            // expected — service throws IllegalStateException (or wraps it in RuntimeException)
        }
    }

    @Provide
    Arbitrary<ListingRequest> validListingRequests() {
        return Combinators.combine(
            Arbitraries.integers().between(1, 1000).map(BigDecimal::valueOf),
            Arbitraries.integers().between(1, 100).map(BigDecimal::valueOf)
        ).as((qty, price) -> {
            ListingRequest req = new ListingRequest();
            req.setFarmerId(UUID.randomUUID());
            req.setCropType("Wheat");
            req.setQuantity(qty);
            req.setPricePerUnit(price);
            req.setLocation("Test Location");
            return req;
        });
    }

    @Provide
    Arbitrary<String> inactiveStatuses() {
        return Arbitraries.of("Pending_Verification", "Inactive", "Locked", "NOT_FOUND");
    }
}
