package com.agrichain.reporting;

import com.agrichain.reporting.dto.DashboardResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    /**
     * GET /reports/dashboard
     * Public KPI dashboard.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(@RequestHeader(value = "X-User-ID", required = false) String userId) {
        UUID currentUserId = (userId != null) ? UUID.fromString(userId) : UUID.randomUUID();
        DashboardResponse response = reportingService.generateDashboard(currentUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<java.util.List<com.agrichain.reporting.entity.ReportMetadata>> listReports() {
        return ResponseEntity.ok(reportingService.listReports());
    }

    /**
     * POST /reports
     * Request a periodic report.
     */
    @PostMapping
    public ResponseEntity<UUID> generateReport(
            @RequestHeader("X-User-ID") UUID userId,
            @jakarta.validation.Valid @RequestBody com.agrichain.reporting.dto.ReportRequest request) {
        UUID reportId = reportingService.generateReport(request, userId);
        return ResponseEntity.status(202).body(reportId);
    }

    /**
     * GET /reports/{id}/export
     * Download report file.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable UUID id) {
        byte[] fileData = reportingService.getReportFile(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"report-" + id + ".csv\"")
                .body(fileData);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
