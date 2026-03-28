package com.agrichain.reporting;

import com.agrichain.reporting.dto.DashboardResponse;
import com.agrichain.reporting.repository.ReportMetadataRepository;
import net.jqwik.api.*;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class ReportingPropertyTest {

    private final ReportMetadataRepository metadataRepository = Mockito.mock(ReportMetadataRepository.class);
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final ReportingService reportingService;

    public ReportingPropertyTest() {
        RestTemplateBuilder builder = Mockito.mock(RestTemplateBuilder.class);
        when(builder.build()).thenReturn(restTemplate);
        this.reportingService = new ReportingService(metadataRepository, builder);
    }

    /**
     * Property 31: Dashboard correctly aggregates metrics from all four source services.
     */
    @Property(tries = 50)
    void property_31_dashboard_aggregation(
            @ForAll long activeFarmerCount,
            @ForAll long totalCropVolume,
            @ForAll BigDecimal totalTransactionValue,
            @ForAll BigDecimal totalSubsidiesDisbursed) {

        Mockito.reset(restTemplate);

        when(restTemplate.getForObject(contains("/farmers/count"), eq(Long.class))).thenReturn(activeFarmerCount);
        when(restTemplate.getForObject(contains("/listings/total-volume"), eq(Long.class))).thenReturn(totalCropVolume);
        when(restTemplate.getForObject(contains("/transactions/total-value"), eq(BigDecimal.class))).thenReturn(totalTransactionValue);
        when(restTemplate.getForObject(contains("/subsidies/total-disbursed"), eq(BigDecimal.class))).thenReturn(totalSubsidiesDisbursed);

        DashboardResponse response = reportingService.generateDashboard(UUID.randomUUID());

        assertThat(response.getActiveFarmerCount()).isEqualTo(activeFarmerCount);
        assertThat(response.getTotalCropVolume()).isEqualTo(totalCropVolume);
        assertThat(response.getTotalTransactionValue()).isEqualTo(totalTransactionValue);
        assertThat(response.getTotalSubsidiesDisbursed()).isEqualTo(totalSubsidiesDisbursed);
    }
}
