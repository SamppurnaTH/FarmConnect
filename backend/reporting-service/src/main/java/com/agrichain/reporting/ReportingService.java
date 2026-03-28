package com.agrichain.reporting;

import com.agrichain.reporting.dto.DashboardResponse;
import com.agrichain.reporting.entity.ReportMetadata;
import com.agrichain.reporting.repository.ReportMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ReportingService {

    private final ReportMetadataRepository metadataRepository;
    private final RestTemplate restTemplate;

    @Value("${services.farmer.url}")
    private String farmerServiceUrl;

    @Value("${services.crop.url}")
    private String cropServiceUrl;

    @Value("${services.transaction.url}")
    private String transactionServiceUrl;

    @Value("${services.subsidy.url}")
    private String subsidyServiceUrl;

    public ReportingService(ReportMetadataRepository metadataRepository, RestTemplateBuilder restTemplateBuilder) {
        this.metadataRepository = metadataRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    /**
     * Requirement 15.1-15.4: Generate KPI Dashboard.
     */
    @Transactional
    public DashboardResponse generateDashboard(UUID requesterId) {
        DashboardResponse response = new DashboardResponse();

        // 1. Farmer Count (Active)
        try {
            Long farmerCount = restTemplate.getForObject(farmerServiceUrl + "/farmers/count?status=Active", Long.class);
            response.setActiveFarmerCount(farmerCount != null ? farmerCount : 0L);
        } catch (Exception e) {
            response.setActiveFarmerCount(-1L); // Error indicator
        }

        // 2. Crop Volume (Total Active)
        try {
            Long cropVolume = restTemplate.getForObject(cropServiceUrl + "/listings/total-volume", Long.class);
            response.setTotalCropVolume(cropVolume != null ? cropVolume : 0L);
        } catch (Exception e) {
            response.setTotalCropVolume(-1L);
        }

        // 3. Transaction Value (Settled)
        try {
            BigDecimal txValue = restTemplate.getForObject(transactionServiceUrl + "/transactions/total-value", BigDecimal.class);
            response.setTotalTransactionValue(txValue != null ? txValue : BigDecimal.ZERO);
        } catch (Exception e) {
            response.setTotalTransactionValue(BigDecimal.valueOf(-1));
        }

        // 4. Subsidy Disbursed (Total)
        try {
            BigDecimal subsidyValue = restTemplate.getForObject(subsidyServiceUrl + "/subsidies/total-disbursed", BigDecimal.class);
            response.setTotalSubsidiesDisbursed(subsidyValue != null ? subsidyValue : BigDecimal.ZERO);
        } catch (Exception e) {
            response.setTotalSubsidiesDisbursed(BigDecimal.valueOf(-1));
        }

        // Requirement 15.6: Log Metadata
        logMetadata(requesterId, "KPI_DASHBOARD");

        return response;
    }

    /**
     * Requirement 15.2-15.4: Generate periodic report.
     */
    @Transactional
    public UUID generateReport(com.agrichain.reporting.dto.ReportRequest request, UUID requesterId) {
        // 1. Validate date range (Requirement 15.2)
        long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());
        if (months < 0 || months > 12) {
            throw new IllegalArgumentException("Report range must be between 0 and 12 months.");
        }

        // 2. Fetch data (Mocked for now)
        // In reality: call transactionServiceUrl + "/transactions?start=" + request.getStartDate() ...
        String dataSummary = "Report Scope: " + request.getScope() + "\n" +
                           "Period: " + request.getStartDate() + " to " + request.getEndDate() + "\n" +
                           "Total Records: 152\n" +
                           "Summary Value: 45,000.00";
        System.out.println(dataSummary);

        // 3. Log Metadata (Requirement 15.4)
        ReportMetadata metadata = new ReportMetadata();
        metadata.setScope(request.getScope());
        metadata.setGeneratedBy(requesterId);
        metadata.setFormat(request.getFormat());
        metadata = metadataRepository.save(metadata);

        return metadata.getId();
    }

    public byte[] getReportFile(UUID reportId) {
        ReportMetadata metadata = metadataRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        String content = "FarmConnect Report\n" +
                        "ID: " + metadata.getId() + "\n" +
                        "Scope: " + metadata.getScope() + "\n" +
                        "Generated At: " + metadata.getGenerationTimestamp() + "\n\n" +
                        "--- DATA SUMMARY ---\n" +
                        "Sample record 1, 2024-01-01, 500.00\n" +
                        "Sample record 2, 2024-02-01, 1200.00";

        return content.getBytes();
    }

    public java.util.List<ReportMetadata> listReports() {
        return metadataRepository.findAll();
    }

    private void logMetadata(UUID requesterId, String scope) {
        ReportMetadata metadata = new ReportMetadata();
        metadata.setScope(scope);
        metadata.setGeneratedBy(requesterId);
        metadata.setFormat("JSON");
        metadataRepository.save(metadata);
    }
}
