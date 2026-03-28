package com.agrichain.farmer.entity;

import com.agrichain.common.crypto.EncryptedStringConverter;
import com.agrichain.common.enums.FarmerStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "farmers")
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** AES-256 encrypted */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "name", length = 500, nullable = false)
    private String name;

    /** AES-256 encrypted; stored as text to accommodate encrypted bytes */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "date_of_birth", length = 500, nullable = false)
    private String dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    /** AES-256 encrypted */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "address", columnDefinition = "text", nullable = false)
    private String address;

    /** AES-256 encrypted; unique constraint on encrypted value */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "contact_info", length = 500, nullable = false)
    private String contactInfo;

    @Column(name = "land_details", columnDefinition = "text")
    private String landDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FarmerStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (status == null) {
            status = FarmerStatus.Pending_Verification;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getLandDetails() { return landDetails; }
    public void setLandDetails(String landDetails) { this.landDetails = landDetails; }

    public FarmerStatus getStatus() { return status; }
    public void setStatus(FarmerStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
