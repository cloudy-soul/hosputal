package com.hospital.service;

import com.hospital.dto.GenerateReportRequest;
import com.hospital.entity.MaintenanceReport;
import com.hospital.entity.MaintenanceTechnician;
import com.hospital.entity.WorkOrder;
import com.hospital.entity.WorkOrder.WorkOrderStatus;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.MaintenanceReportRepository;
import com.hospital.repository.WorkOrderRepository;
import com.hospital.service.AuditService;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintenance Report Service – generates and retrieves formal completion records.
 *
 * <p>Reports are the primary compliance artefact consumed by:</p>
 * <ul>
 *   <li><b>Finance</b>    – reads hours spent and actual cost for cost allocation.</li>
 *   <li><b>Management</b> – department-level maintenance statistics.</li>
 *   <li><b>HR</b>         – technician work summary feeds into payroll hours.</li>
 *   <li><b>Accreditation</b> – JCI auditors review completion records.</li>
 * </ul>
 */
@Service
@Transactional
public class MaintenanceReportService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceReportService.class);

    private final MaintenanceReportRepository reportRepo;
    private final WorkOrderRepository workOrderRepo;
    private final MaintenanceTechnicianService technicianService;
    private final AuditService auditService;
    private final IdGenerator idGenerator;

    // Explicit constructor to avoid Lombok-generated constructor and IDE annotation-processor issues
    public MaintenanceReportService(MaintenanceReportRepository reportRepo,
                                    WorkOrderRepository workOrderRepo,
                                    MaintenanceTechnicianService technicianService,
                                    AuditService auditService,
                                    IdGenerator idGenerator) {
        this.reportRepo = reportRepo;
        this.workOrderRepo = workOrderRepo;
        this.technicianService = technicianService;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    // ── Generate ─────────────────────────────────────────────────────

    /**
     * Generates a formal maintenance report for a completed work order.
     *
     * <p>Called manually when the technician needs to file the report separately
     * from the work-order completion flow (e.g. multi-day jobs requiring sign-off).</p>
     */
    public Map<String, Object> generateReport(Long workOrderId,
                                              GenerateReportRequest req,
                                              String technicianEmail) {
        WorkOrder wo = workOrderRepo.findById(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + workOrderId));

        if (wo.getStatus() != WorkOrderStatus.COMPLETED) {
            throw new BadRequestException(
                "Reports can only be generated for COMPLETED work orders. Current status: " + wo.getStatus());
        }

        if (reportRepo.findByWorkOrderId(workOrderId).isPresent()) {
            throw new BadRequestException(
                "A report already exists for work order: " + wo.getWorkOrderNumber());
        }

        MaintenanceTechnician tech = technicianService.findByEmail(technicianEmail);

        MaintenanceReport report = new MaintenanceReport();
        report.setReportNumber(idGenerator.generateMaintenanceReportNumber());
        report.setWorkOrder(wo);
        report.setTechnician(tech);
        report.setWorkPerformed(req.getWorkPerformed());
        report.setHoursSpent(req.getHoursSpent());
        report.setSignatureRequired(req.isSignatureRequired());
        report.setPatientAreaSignatory(req.getPatientAreaSignatory());

        MaintenanceReport saved = reportRepo.save(report);

        auditService.log(tech.getId(), "GENERATE_MAINTENANCE_REPORT",
                "MaintenanceReport", saved.getId(), "SYSTEM",
                saved.getReportNumber() + " for " + wo.getWorkOrderNumber());

        log.info("Maintenance report {} generated for work order {}",
            saved.getReportNumber(), wo.getWorkOrderNumber());

        return toReportMap(saved);
    }

    // ── Queries ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getReportByWorkOrder(Long workOrderId) {
        MaintenanceReport report = reportRepo.findByWorkOrderId(workOrderId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "No report found for work order: " + workOrderId));
        return toReportMap(report);
    }

    /**
     * Returns a summary of work completed by a technician in a date range.
     * Used by HR for payroll and by Management for performance reviews.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getTechnicianWorkSummary(Long technicianId,
                                                         LocalDate start,
                                                         LocalDate end) {
        MaintenanceTechnician tech = technicianService.findById(technicianId);

        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt   = end.atTime(23, 59, 59);

        List<MaintenanceReport> reports = reportRepo.findByTechnicianId(technicianId).stream()
            .filter(r -> !r.getCompletedAt().isBefore(startDt) && !r.getCompletedAt().isAfter(endDt))
            .toList();

        BigDecimal totalHours = reports.stream()
            .map(MaintenanceReport::getHoursSpent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCost = reports.stream()
            .map(r -> r.getWorkOrder().getActualCost() != null
                    ? r.getWorkOrder().getActualCost() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("technicianId", tech.getTechnicianId());
        summary.put("technicianName", tech.getFullName());
        summary.put("specialization", tech.getSpecialization());
        summary.put("period", Map.of("from", start.toString(), "to", end.toString()));
        summary.put("totalJobsCompleted", reports.size());
        summary.put("totalHoursSpent", totalHours);
        summary.put("totalActualCost", totalCost);
        summary.put("reports", reports.stream().map(this::toReportMap).toList());
        return summary;
    }

    /**
     * Returns maintenance statistics by department – which departments generate
     * the most work orders, their costs, and resolution times.
     *
     * <p>Used by Management for budget planning and by Finance for cost allocation.</p>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDepartmentStats(LocalDate start, LocalDate end) {
        LocalDateTime startDt = start.atStartOfDay();
        LocalDateTime endDt   = end.atTime(23, 59, 59);

        List<WorkOrder> orders = workOrderRepo.findByReportedDateBetween(startDt, endDt);

        // Group by department
        Map<String, Long> countByDept   = new LinkedHashMap<>();
        Map<String, BigDecimal> costByDept = new LinkedHashMap<>();

        for (WorkOrder wo : orders) {
            String dept = wo.getDepartment() != null ? wo.getDepartment() : "Unknown";
            countByDept.merge(dept, 1L, Long::sum);
            if (wo.getActualCost() != null) {
                costByDept.merge(dept, wo.getActualCost(), BigDecimal::add);
            }
        }

        // Count by status
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (WorkOrder.WorkOrderStatus s : WorkOrder.WorkOrderStatus.values()) {
            long cnt = orders.stream().filter(w -> w.getStatus() == s).count();
            if (cnt > 0) byStatus.put(s.name(), cnt);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("period", Map.of("from", start.toString(), "to", end.toString()));
        stats.put("totalWorkOrders", orders.size());
        stats.put("byStatus", byStatus);
        stats.put("workOrdersByDepartment", countByDept);
        stats.put("costByDepartment", costByDept);
        stats.put("totalCost", costByDept.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return stats;
    }

    // ── Mapper ───────────────────────────────────────────────────────

    Map<String, Object> toReportMap(MaintenanceReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("reportNumber", r.getReportNumber());
        m.put("workOrderNumber", r.getWorkOrder().getWorkOrderNumber());
        m.put("workOrderTitle", r.getWorkOrder().getTitle());
        m.put("technicianId", r.getTechnician().getTechnicianId());
        m.put("technicianName", r.getTechnician().getFullName());
        m.put("workPerformed", r.getWorkPerformed());
        m.put("hoursSpent", r.getHoursSpent());
        m.put("completedAt", r.getCompletedAt());
        m.put("signatureRequired", r.isSignatureRequired());
        m.put("patientAreaSignatory", r.getPatientAreaSignatory());
        m.put("actualCost", r.getWorkOrder().getActualCost());
        return m;
    }
}
