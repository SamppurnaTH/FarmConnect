package com.agrichain.farmer;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.farmer.dto.FarmerRegistrationRequest;
import com.agrichain.farmer.entity.Farmer;
import com.agrichain.farmer.repository.FarmerRepository;
import net.jqwik.api.*;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class FarmerPropertyTest {

    private final FarmerRepository farmerRepository = Mockito.mock(FarmerRepository.class);
    private final FarmerService farmerService;

    public FarmerPropertyTest() {
        // We mock dependencies for the service
        org.springframework.boot.web.client.RestTemplateBuilder builder = Mockito.mock(org.springframework.boot.web.client.RestTemplateBuilder.class);
        org.springframework.web.client.RestTemplate restTemplate = Mockito.mock(org.springframework.web.client.RestTemplate.class);
        com.agrichain.farmer.repository.FarmerDocumentRepository documentRepository = Mockito.mock(com.agrichain.farmer.repository.FarmerDocumentRepository.class);
        when(builder.build()).thenReturn(restTemplate);
        
        // Mock Identity Service response
        when(restTemplate.postForObject(any(), any(), any())).thenReturn(UUID.randomUUID());

        this.farmerService = new FarmerService(farmerRepository, documentRepository, builder);
    }

    /**
     * Property 10: Valid farmer registration creates a Pending Verification record.
     */
    @Property
    void validRegistrationSetsPendingStatus(@ForAll("validRequests") FarmerRegistrationRequest request) {
        Mockito.reset(farmerRepository);
        // Setup mock repository to return the entity as saved
        when(farmerRepository.save(any(Farmer.class))).thenAnswer(invocation -> {
            Farmer f = invocation.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        when(farmerRepository.existsByContactInfoAndStatusIn(any(), any())).thenReturn(false);

        UUID id = farmerService.registerFarmer(request);
        
        Assertions.assertNotNull(id);
        // Verify status was set to Pending_Verification
        Mockito.verify(farmerRepository).save(Mockito.argThat(farmer -> 
            farmer.getStatus() == FarmerStatus.Pending_Verification
        ));
    }

    /**
     * Property 12: Duplicate contact information is rejected.
     */
    @Property
    void duplicateContactInfoIsRejected(@ForAll("validRequests") FarmerRegistrationRequest request) {
        // Setup mock: contact information already exists
        when(farmerRepository.existsByContactInfoAndStatusIn(any(), any())).thenReturn(true);

        Assertions.assertThrows(IllegalArgumentException.class, () -> farmerService.registerFarmer(request));
    }

    @Provide
    Arbitrary<FarmerRegistrationRequest> validRequests() {
        return Combinators.combine(
            Arbitraries.strings().alpha().ofMinLength(3).ofMaxLength(20), // username
            Arbitraries.strings().all().ofMinLength(8).ofMaxLength(20),     // password
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20).map(s -> s + "@example.com"), // email
            Arbitraries.strings().alpha().ofMinLength(2).ofMaxLength(50)  // name
        ).as((user, pass, email, name) -> {
            FarmerRegistrationRequest req = new FarmerRegistrationRequest();
            req.setUsername(user);
            req.setPassword(pass);
            req.setEmail(email);
            req.setName(name);
            req.setDateOfBirth(LocalDate.now().minusYears(20));
            req.setGender("Other");
            req.setAddress("Test Address");
            req.setContactInfo(UUID.randomUUID().toString()); // unique
            req.setLandDetails("Test Land");
            return req;
        });
    }
}
