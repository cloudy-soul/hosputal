package com.hospital.controller;

import com.hospital.service.MaintenanceDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Maintenance Dashboard Controller.
 *
 * <p>Base path: {@code /api/maintenance/dashboard}</p>
 *
 * <pre>
 * GET /stats          – Full dashboard: counts, emergencies, overdue, low-stock [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET /recent         – Most recent work orders (default 10)                    [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET /workload       – Per-technician open order count                         [ADMIN, MANAGEMENT]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
public class MaintenanceDashboardController {

    private final MaintenanceDashboardService dashboardService;

    // Explicit constructor to avoid Lombok annotation-processor dependency in the IDE
    public MaintenanceDashboardController(MaintenanceDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/maintenance/dashboard/stats
     *  Returns a real-time operational summary for the maintenance manager:
     *  work order counts by status/priority, active emergencies,
     *  overdue preventive tasks, low stock alerts, and technician workloads. */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /** GET /api/maintenance/dashboard/recent?limit=10
     *  Returns the most recently created work orders for the activity feed. */
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> recent(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(dashboardService.getRecentWorkOrders(limit));
    }
}
