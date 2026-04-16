package com.agrichain.farmer.dto;

import com.agrichain.common.enums.FarmerStatus;
import com.agrichain.farmer.entity.Farmer;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe response DTO for farmer profile.
 * Never exposes the raw JPA entity — prevents mass-assignment and
 * accidental serialisation of internal fields.
 */
public class FarmerProfileResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String contactInfo;
    private String landDetails;
    private FarmerStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    /** Factory method — maps from entity to DTO. */
    public static FarmerProfileResponse from(Farmer f) {
        FarmerProfileResponse dto = new FarmerProfileResponse();
        dto.id          = f.getId();
        dto.userId      = f.getUserId();
        dto.name        = f.getName();
        dto.dateOfBirth = f.getDateOfBirth();
        dto.gender      = f.getGender();
        dto.address     = f.getAddress();
        dto.contactInfo = f.getContactInfo();
        dto.landDetails = f.getLandDetails();
        dto.status      = f.getStatus();
        dto.createdAt   = f.getCreatedAt();
        dto.updatedAt   = f.getUpdatedAt();
        return dto;
    }

    public UUID getId()            { return id; }
    public UUID getUserId()        { return userId; }
    public String getName()        { return name; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getGender()      { return gender; }
    public String getAddress()     { return address; }
    public String getContactInfo() { return contactInfo; }
    public String getLandDetails() { return landDetails; }
    public FarmerStatus getStatus(){ return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}
