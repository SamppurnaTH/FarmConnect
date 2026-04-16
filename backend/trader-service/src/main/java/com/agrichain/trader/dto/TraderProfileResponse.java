package com.agrichain.trader.dto;

import com.agrichain.common.enums.UserStatus;
import com.agrichain.trader.entity.Trader;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe response DTO for trader profile.
 * Never exposes the raw JPA entity.
 */
public class TraderProfileResponse {

    private UUID id;
    private UUID userId;
    private String name;
    private String organization;
    private String contactInfo;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public static TraderProfileResponse from(Trader t) {
        TraderProfileResponse dto = new TraderProfileResponse();
        dto.id           = t.getId();
        dto.userId       = t.getUserId();
        dto.name         = t.getName();
        dto.organization = t.getOrganization();
        dto.contactInfo  = t.getContactInfo();
        dto.status       = t.getStatus();
        dto.createdAt    = t.getCreatedAt();
        dto.updatedAt    = t.getUpdatedAt();
        return dto;
    }

    public UUID getId()            { return id; }
    public UUID getUserId()        { return userId; }
    public String getName()        { return name; }
    public String getOrganization(){ return organization; }
    public String getContactInfo() { return contactInfo; }
    public UserStatus getStatus()  { return status; }
    public Instant getCreatedAt()  { return createdAt; }
    public Instant getUpdatedAt()  { return updatedAt; }
}
