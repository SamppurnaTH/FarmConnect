package com.agrichain.farmer.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO for comprehensive farmer registration, including user account creation.
 */
public class FarmerRegistrationRequest {

    // --- User Credentials ---
    @NotBlank
    @Size(min = 3, max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 255)
    private String password;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // --- Farmer Profile ---
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    private String gender;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotBlank
    @Size(max = 20)
    private String contactInfo;

    @NotBlank
    @Size(max = 1000)
    private String landDetails;

    // Getters and setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }
    public String getLandDetails() { return landDetails; }
    public void setLandDetails(String landDetails) { this.landDetails = landDetails; }
}
