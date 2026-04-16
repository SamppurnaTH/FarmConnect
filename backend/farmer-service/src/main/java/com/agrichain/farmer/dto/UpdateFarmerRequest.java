package com.agrichain.farmer.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for farmer profile updates.
 * Only exposes the fields a farmer is allowed to change.
 * Prevents mass-assignment of status, userId, or audit fields.
 */
public class UpdateFarmerRequest {

    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 20, message = "Contact info must not exceed 20 characters")
    private String contactInfo;

    @Size(max = 1000, message = "Land details must not exceed 1000 characters")
    private String landDetails;

    public String getName()        { return name; }
    public void setName(String n)  { this.name = n; }

    public String getAddress()           { return address; }
    public void setAddress(String a)     { this.address = a; }

    public String getContactInfo()           { return contactInfo; }
    public void setContactInfo(String c)     { this.contactInfo = c; }

    public String getLandDetails()           { return landDetails; }
    public void setLandDetails(String l)     { this.landDetails = l; }
}
