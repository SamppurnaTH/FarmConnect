package com.agrichain.reporting;

import com.agrichain.reporting.dto.DashboardResponse;
import com.agrichain.reporting.dto.ReportDataRow;
import com.agrichain.reporting.entity.ReportMetadata;
import com.agrichain.reporting.repository.ReportMetadataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
     * Fetches real data from downstream services filtered by date range.
     */
    @Transactional
    public UUID generateReport(com.agrichain.reporting.dto.ReportRequest request, UUID requesterId) {
        // 1. Validate date range (Requirement 15.2)
        long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());
        if (months < 0 || months > 12) {
            throw new IllegalArgumentException("Report range must be between 0 and 12 months.");
        }

        // 2. Log Metadata (Requirement 15.4) — persist before fetching so we always have a record
        ReportMetadata metadata = new ReportMetadata();
        metadata.setScope(request.getScope().toUpperCase());
        metadata.setGeneratedBy(requesterId);
        metadata.setFormat(request.getFormat());
        metadata.setDateRangeStart(request.getStartDate());
        metadata.setDateRangeEnd(request.getEndDate());
        metadata = metadataRepository.save(metadata);

        return metadata.getId();
    }

    /**
     * Builds a CSV report by fetching real data from downstream services.
     * Scope determines which service is queried.
     */
    public byte[] getReportFile(UUID reportId) {
        ReportMetadata metadata = metadataRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        List<ReportDataRow> rows = fetchReportData(metadata);

        StringBuilder csv = new StringBuilder();
        csv.append("FarmConnect Report\n");
        csv.append("ID,").append(metadata.getId()).append("\n");
        csv.append("Scope,").append(metadata.getScope()).append("\n");
        csv.append("Period,").append(metadata.getDateRangeStart())
           .append(" to ").append(metadata.getDateRangeEnd()).append("\n");
        csv.append("Generated At,").append(metadata.getGenerationTimestamp()).append("\n\n");
        csv.append("--- DATA ---\n");
        csv.append("ID,Date,Description,Amount,Status\n");

        for (ReportDataRow row : rows) {
            csv.append(escapeCsv(row.getId())).append(",")
               .append(escapeCsv(row.getDate())).append(",")
               .append(escapeCsv(row.getDescription())).append(",")
               .append(escapeCsv(row.getAmount())).append(",")
               .append(escapeCsv(row.getStatus())).append("\n");
        }

        if (rows.isEmpty()) {
            csv.append("No records found for the selected period.\n");
        }

        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Fetches data rows from the appropriate downstream service based on report scope.
     */
    @SuppressWarnings("unchecked")
    private List<ReportDataRow> fetchReportData(ReportMetadata metadata) {
        String scope = metadata.getScope() != null ? metadata.getScope().toUpperCase() : "";
        LocalDate start = metadata.getDateRangeStart();
        LocalDate end   = metadata.getDateRangeEnd();

        List<ReportDataRow> rows = new ArrayList<>();

        try {
            if (scope.contains("TRANSACTION")) {
                String url = UriComponentsBuilder.fromHttpUrl(transactionServiceUrl + "/transactions/report")
                        .queryParam("start", start).queryParam("end", end).toUriString();
                List<Map<String, Object>> data = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        rows.add(new ReportDataRow(
                                str(item.get("id")),
                                str(item.get("createdAt")),
                                "Order: " + str(item.get("orderId")),
                                str(item.get("amount")),
                                str(item.get("status"))
                        ));
                    }
                }
            } else if (scope.contains("SUBSID") || scope.contains("DISBURSEMENT")) {
                String url = UriComponentsBuilder.fromHttpUrl(subsidyServiceUrl + "/subsidies/disbursements/report")
                        .queryParam("start", start).queryParam("end", end).toUriString();
                List<Map<String, Object>> data = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        rows.add(new ReportDataRow(
                                str(item.get("id")),
                                str(item.get("createdAt")),
                                "Program: " + str(item.get("programId")) + " | Cycle: " + str(item.get("programCycle")),
                                str(item.get("amount")),
                                str(item.get("status"))
                        ));
                    }
                }
            } else if (scope.contains("FARMER")) {
                String url = UriComponentsBuilder.fromHttpUrl(farmerServiceUrl + "/farmers/report")
                        .queryParam("start", start).queryParam("end", end).toUriString();
                List<Map<String, Object>> data = restTemplate.exchange(
                        url, HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (data != null) {
                    for (Map<String, Object> item : data) {
                        rows.add(new ReportDataRow(
                                str(item.get("id")),
                                str(item.get("createdAt")),
                                str(item.get("name")),
                                "",
                                str(item.get("status"))
                        ));
                    }
                }
            } else {
                // Generic: return KPI summary as rows
                DashboardResponse kpis = generateDashboard(metadata.getGeneratedBy());
                rows.add(new ReportDataRow("kpi-1", start != null ? start.toString() : "", "Active Farmers",
                        String.valueOf(kpis.getActiveFarmerCount()), ""));
                rows.add(new ReportDataRow("kpi-2", start != null ? start.toString() : "", "Total Crop Volume (kg)",
                        String.valueOf(kpis.getTotalCropVolume()), ""));
                rows.add(new ReportDataRow("kpi-3", start != null ? start.toString() : "", "Total Transaction Value",
                        kpis.getTotalTransactionValue().toPlainString(), "Settled"));
                rows.add(new ReportDataRow("kpi-4", start != null ? start.toString() : "", "Total Subsidies Disbursed",
                        kpis.getTotalSubsidiesDisbursed().toPlainString(), "Disbursed"));
            }
        } catch (Exception e) {
            System.err.println("[reporting] Failed to fetch report data for scope=" + scope + ": " + e.getMessage());
            // Return empty rows — the CSV will note no records found
        }

        return rows;
    }

    private String str(Object o) {
        return o != null ? o.toString() : "";
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
