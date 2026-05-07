package com.hospital.service;

import com.hospital.entity.WorkOrder.WorkOrderStatus;
import com.hospital.entity.WorkOrder.Priority;
import com.hospital.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dashboard Service – aggregates real-time statistics for the Maintenance Department.
 *
 * <p>Consumed by:</p>
 * <ul>
 *   <li>Maintenance Manager – operational overview (open orders, emergencies, overdue tasks)</li>
 *   <li>Hospital Management – KPI reporting (work order volumes, response times)</li>
 *   <li>Finance – pending cost estimates vs actuals</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MaintenanceDashboardService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceDashboardService.class);

    private final WorkOrderRepository workOrderRepo;
    private final PreventiveMaintenanceTaskRepository taskRepo;
    private final SparePartRepository sparePartRepo;
    private final MaintenanceTechnicianRepository techRepo;
    private final WorkOrderService workOrderService;

    // Explicit constructor to avoid Lombok annotation-processor reliance in the IDE
    public MaintenanceDashboardService(WorkOrderRepository workOrderRepo,
                                       PreventiveMaintenanceTaskRepository taskRepo,
                                       SparePartRepository sparePartRepo,
                                       MaintenanceTechnicianRepository techRepo,
                                       WorkOrderService workOrderService) {
        this.workOrderRepo = workOrderRepo;
        this.taskRepo = taskRepo;
        this.sparePartRepo = sparePartRepo;
        this.techRepo = techRepo;
        this.workOrderService = workOrderService;
    }

    /**
     * Returns a full dashboard summary:
     * work-order counts by status, emergency alerts, overdue preventive tasks,
     * low-stock spare parts, and per-technician workloads.
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> dash = new LinkedHashMap<>();

        // ── Work order counts by status ──────────────────────────────
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WorkOrderStatus s : WorkOrderStatus.values()) {
            byStatus.put(s.name(), workOrderRepo.countByStatus(s));
        }
        dash.put("workOrdersByStatus", byStatus);

        // ── Priority counts ──────────────────────────────────────────
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) {
            byPriority.put(p.name(), workOrderRepo.countByPriority(p));
        }
        dash.put("workOrdersByPriority", byPriority);

        // ── Active emergencies ───────────────────────────────────────
        List<Map<String, Object>> emergencies = workOrderRepo
            .findActiveEmergencyOrders()
            .stream()
            .map(workOrderService::toWorkOrderMap)
            .toList();
        dash.put("activeEmergencies", emergencies);
        dash.put("activeEmergencyCount", emergencies.size());

        // ── Overdue preventive tasks ─────────────────────────────────
        long overdueTasks = taskRepo.findAllOverdueTasks().size();
        dash.put("overduePreventiveTaskCount", overdueTasks);

        // ── Low stock spare parts ────────────────────────────────────
        long lowStockCount = sparePartRepo.findLowStockParts().size();
        dash.put("lowStockSparePartCount", lowStockCount);

        // ── Technician summary ───────────────────────────────────────
        long totalTechs  = techRepo.count();
        long onCallCount = techRepo.findByIsOnCallTrue().size();
        dash.put("totalTechnicians", totalTechs);
        dash.put("onCallTechnicians", onCallCount);

        // ── Technician workload (assigned open orders per tech) ──────
        List<Map<String, Object>> workload = techRepo.findAll().stream()
            .map(tech -> {
                long open = workOrderRepo
                    .findByAssignedToIdAndStatus(tech.getId(), WorkOrderStatus.ASSIGNED).size()
                    + workOrderRepo
                    .findByAssignedToIdAndStatus(tech.getId(), WorkOrderStatus.IN_PROGRESS).size();
                Map<String, Object> tw = new LinkedHashMap<>();
                tw.put("technicianId", tech.getTechnicianId());
                tw.put("name", tech.getFullName());
                tw.put("specialization", tech.getSpecialization());
                tw.put("isOnCall", tech.isOnCall());
                tw.put("openWorkOrders", open);
                return tw;
            }).toList();
        dash.put("technicianWorkload", workload);

        return dash;
    }

    /**
     * Returns the most recently created work orders for the dashboard feed.
     */
    public List<Map<String, Object>> getRecentWorkOrders(int limit) {
        return workOrderRepo
            .findAll(PageRequest.of(0, Math.min(limit, 50)))
            .getContent()
            .stream()
            .map(workOrderService::toWorkOrderMap)
            .toList();
    }
}
