package com.hospital.controller;

import com.hospital.dto.GenerateReportRequest;
import com.hospital.service.MaintenanceReportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Maintenance Report Controller.
 *
 * <p>Base path: {@code /api/maintenance/reports}</p>
 *
 * <pre>
 * POST /work-order/{id}              – Generate report for completed order  [MAINTENANCE]
 * GET  /work-order/{id}              – Get report by work order             [ADMIN, MANAGEMENT, MAINTENANCE, FINANCE]
 * GET  /technician/{id}/summary      – Technician work summary (HR/payroll) [ADMIN, MANAGEMENT, FINANCE]
 * GET  /department-stats             – Which departments report most issues  [ADMIN, MANAGEMENT, FINANCE]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE', 'FINANCE')")
public class MaintenanceReportController {

    private final MaintenanceReportService reportService;

    // Explicit constructor to avoid Lombok annotation-processor dependency in the IDE
    public MaintenanceReportController(MaintenanceReportService reportService) {
        this.reportService = reportService;
    }

    /** POST /api/maintenance/reports/work-order/{id}
     *  Generates a formal completion report for a COMPLETED work order.
     *  Used when the report must be filed separately (e.g. multi-day jobs).
     *
     *  <p>Example request body:</p>
     *  <pre>
     *  {
     *    "workPerformed": "Replaced capacitor and recharged refrigerant. Tested for 2 hours.",
     *    "hoursSpent": 3.5,
     *    "signatureRequired": true,
     *    "patientAreaSignatory": "Dr. Sarah Chen – Head of Cardiology"
     *  }
     *  </pre>
     */
    @PostMapping("/work-order/{id}")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> generateReport(
            @PathVariable Long id,
            @Valid @RequestBody GenerateReportRequest request,
            Authentication auth) {
        return ResponseEntity.ok(reportService.generateReport(id, request, auth.getName()));
    }

    /** GET /api/maintenance/reports/work-order/{id} */
    @GetMapping("/work-order/{id}")
    public ResponseEntity<Map<String, Object>> getByWorkOrder(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportByWorkOrder(id));
    }

    /** GET /api/maintenance/reports/technician/{id}/summary?from=2025-01-01&to=2025-03-31
     *  Returns hours worked, jobs completed, and total cost for a technician in a date range.
     *  Used by HR for payroll and by Management for performance reviews. */
    @GetMapping("/technician/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'FINANCE', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> technicianSummary(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getTechnicianWorkSummary(id, from, to));
    }

    /** GET /api/maintenance/reports/department-stats?from=2025-01-01&to=2025-03-31
     *  Returns work order counts, status breakdown, and cost totals per department.
     *  Finance uses this for departmental budget charge-back. */
    @GetMapping("/department-stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'FINANCE', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> departmentStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(reportService.getDepartmentStats(from, to));
    }
}
