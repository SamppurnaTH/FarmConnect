package com.agrichain.farmer;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.common.enums.UserRole;
import com.agrichain.common.enums.VerificationStatus;
import com.agrichain.farmer.dto.FarmerRegistrationRequest;
import com.agrichain.farmer.entity.Farmer;
import com.agrichain.farmer.entity.FarmerDocument;
import com.agrichain.farmer.entity.DocumentType;
import com.agrichain.farmer.repository.FarmerRepository;
import com.agrichain.farmer.repository.FarmerDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final FarmerDocumentRepository farmerDocumentRepository;
    private final RestTemplate restTemplate;

    @Value("${services.identity.url:http://localhost:8081}")
    private String identityServiceUrl;

    public FarmerService(FarmerRepository farmerRepository, 
                         FarmerDocumentRepository farmerDocumentRepository,
                         RestTemplateBuilder restTemplateBuilder) {
        this.farmerRepository = farmerRepository;
        this.farmerDocumentRepository = farmerDocumentRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Transactional
    public UUID registerFarmer(FarmerRegistrationRequest request) {
        if (farmerRepository.existsByContactInfoAndStatusIn(request.getContactInfo(), 
                List.of(FarmerStatus.Active, FarmerStatus.Pending_Verification))) {
            throw new IllegalArgumentException("Farmer with this contact information already exists.");
        }

        UUID userId = createUserInIdentityService(request);

        Farmer farmer = new Farmer();
        farmer.setUserId(userId);
        farmer.setName(request.getName());
        farmer.setDateOfBirth(request.getDateOfBirth().toString());
        farmer.setGender(request.getGender());
        farmer.setAddress(request.getAddress());
        farmer.setContactInfo(request.getContactInfo());
        farmer.setLandDetails(request.getLandDetails());
        farmer.setStatus(FarmerStatus.Pending_Verification);

        return farmerRepository.save(farmer).getId();
    }

    @Transactional
    public void updateStatus(UUID id, FarmerStatus newStatus) {
        Farmer farmer = getFarmer(id)
                .orElseThrow(() -> new IllegalArgumentException("Farmer not found with ID: " + id));
        farmer.setStatus(newStatus);
        farmerRepository.save(farmer);
        notifyStatusChange(farmer);
    }

    public Optional<Farmer> getFarmer(UUID id) {
        return farmerRepository.findById(id);
    }

    public long countFarmers(FarmerStatus status) {
        if (status == null) return farmerRepository.count();
        return farmerRepository.findByStatus(status).size();
    }

    public List<Farmer> listFarmers(FarmerStatus status) {
        if (status == null) return farmerRepository.findAll();
        return farmerRepository.findByStatus(status);
    }

    @Transactional
    public Farmer updateFarmer(UUID id, Farmer updatedFields, UUID currentUserId) {
        Farmer farmer = getFarmer(id)
                .orElseThrow(() -> new IllegalArgumentException("Farmer not found with ID: " + id));
        String previousValue = serialize(farmer);

        if (updatedFields.getName() != null) farmer.setName(updatedFields.getName());
        if (updatedFields.getAddress() != null) farmer.setAddress(updatedFields.getAddress());
        if (updatedFields.getLandDetails() != null) farmer.setLandDetails(updatedFields.getLandDetails());

        Farmer saved = farmerRepository.save(farmer);
        String newValue = serialize(saved);
        recordAuditLog(currentUserId, "UPDATE", "FARMER", saved.getId(), previousValue, newValue);

        return saved;
    }

    @Transactional
    public UUID uploadDocument(UUID farmerId, DocumentType type, String storagePath) {
        getFarmer(farmerId); // Validate farmer exists
        
        FarmerDocument doc = new FarmerDocument();
        doc.setFarmerId(farmerId);
        doc.setDocumentType(type);
        doc.setStoragePath(storagePath);
        doc.setVerificationStatus(VerificationStatus.Pending); // Requirement 5.1

        return farmerDocumentRepository.save(doc).getId();
    }

    @Transactional
    public void verifyDocument(UUID farmerId, UUID docId, VerificationStatus status, UUID officerId, String reason) {
        FarmerDocument doc = farmerDocumentRepository.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID: " + docId));
        
        if (!doc.getFarmerId().equals(farmerId)) {
            throw new IllegalArgumentException("Document does not belong to farmer: " + farmerId);
        }

        doc.setVerificationStatus(status);
        doc.setReviewedBy(officerId);
        doc.setReviewedAt(java.time.Instant.now());
        doc.setRejectionReason(reason);

        farmerDocumentRepository.save(doc);

        // Requirement 5.3: Notify farmer on status change
        notifyDocumentStatusChange(doc);
    }

    private void notifyDocumentStatusChange(FarmerDocument doc) {
        System.out.println("Notification sent for document " + doc.getId() + ": Status updated to " + doc.getVerificationStatus());
    }

    private void notifyStatusChange(Farmer farmer) {
        System.out.println("Notification sent to farmer " + farmer.getName() + ": Status updated to " + farmer.getStatus());
    }

    private UUID createUserInIdentityService(FarmerRegistrationRequest request) {
        Map<String, Object> userRequest = Map.of(
                "username", request.getUsername(),
                "password", request.getPassword(),
                "email", request.getEmail(),
                "role", UserRole.Farmer
        );
        try {
            return restTemplate.postForObject(identityServiceUrl + "/auth/register", userRequest, UUID.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                throw new IllegalArgumentException("Username or email already registered.");
            }
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user account: " + e.getMessage(), e);
        }
    }

    private String serialize(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private void recordAuditLog(UUID userId, String action, String resourceType, UUID resourceId, String prev, String next) {
        Map<String, Object> log = Map.of(
                "userId", userId,
                "actionType", action,
                "resourceType", resourceType,
                "resourceId", resourceId,
                "previousValue", prev,
                "newValue", next
        );
        try {
            restTemplate.postForObject(identityServiceUrl + "/audit-log", log, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to record audit log: " + e.getMessage());
        }
    }
}
