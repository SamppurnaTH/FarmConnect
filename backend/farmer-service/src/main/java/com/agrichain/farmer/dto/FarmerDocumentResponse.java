package com.agrichain.farmer.dto;

import com.agrichain.common.enums.VerificationStatus;
import com.agrichain.farmer.entity.DocumentType;
import com.agrichain.farmer.entity.FarmerDocument;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe response DTO for farmer documents.
 * Deliberately omits storagePath to avoid leaking internal file paths to clients.
 */
public class FarmerDocumentResponse {

    private UUID id;
    private UUID farmerId;
    private DocumentType documentType;
    private VerificationStatus verificationStatus;
    private String rejectionReason;
    private Instant uploadedAt;
    private Instant reviewedAt;

    public static FarmerDocumentResponse from(FarmerDocument doc) {
        FarmerDocumentResponse dto = new FarmerDocumentResponse();
        dto.id                  = doc.getId();
        dto.farmerId            = doc.getFarmerId();
        dto.documentType        = doc.getDocumentType();
        dto.verificationStatus  = doc.getVerificationStatus();
        dto.rejectionReason     = doc.getRejectionReason();
        dto.uploadedAt          = doc.getUploadedAt();
        dto.reviewedAt          = doc.getReviewedAt();
        return dto;
    }

    public UUID getId()                            { return id; }
    public UUID getFarmerId()                      { return farmerId; }
    public DocumentType getDocumentType()          { return documentType; }
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public String getRejectionReason()             { return rejectionReason; }
    public Instant getUploadedAt()                 { return uploadedAt; }
    public Instant getReviewedAt()                 { return reviewedAt; }
}
