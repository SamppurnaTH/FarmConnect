package com.agrichain.farmer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for document upload requests.
 * Replaces the raw Map<String,String> previously used in the controller.
 */
public class DocumentUploadRequest {

    @NotNull(message = "Document type is required")
    private String type;

    @NotBlank(message = "Storage path is required")
    private String storagePath;

    public String getType()              { return type; }
    public void setType(String type)     { this.type = type; }

    public String getStoragePath()               { return storagePath; }
    public void setStoragePath(String path)      { this.storagePath = path; }
}
