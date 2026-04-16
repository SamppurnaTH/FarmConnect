package com.agrichain.trader.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for trader profile updates.
 * Only exposes fields a trader is allowed to change.
 */
public class UpdateTraderRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String organization;

    @Size(max = 255)
    private String contactInfo;

    public String getName()              { return name; }
    public void setName(String n)        { this.name = n; }
    public String getOrganization()      { return organization; }
    public void setOrganization(String o){ this.organization = o; }
    public String getContactInfo()       { return contactInfo; }
    public void setContactInfo(String c) { this.contactInfo = c; }
}
