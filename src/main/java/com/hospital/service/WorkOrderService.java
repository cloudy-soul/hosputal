package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.entity.User;
import com.hospital.entity.*;
import com.hospital.entity.WorkOrder.WorkOrderStatus;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.UserRepository;
import com.hospital.repository.*;
import com.hospital.service.AuditService;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Work Order Service – orchestrates the full maintenance request lifecycle.
 *
 * <p>Lifecycle: PENDING → ASSIGNED → IN_PROGRESS → COMPLETED (or CANCELLED)</p>
 *
 * <p><b>Cross-department integration:</b></p>
 * <ul>
 *   <li>Any authenticated user can create a work order (no department restriction).</li>
 *   <li>On completion, a {@link MaintenanceReport} is created automatically –
 *       Finance reads this for cost accounting.</li>
 *   <li>EMERGENCY orders trigger a log warning; Phase 3 can wire a notification service.</li>
 * </ul>
 */
@Service
@Transactional
public class WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    private final WorkOrderRepository              workOrderRepo;
    private final MaintenanceTechnicianRepository  techRepo;
    private final WorkOrderPartUsageRepository     partUsageRepo;
    private final MaintenanceReportRepository      reportRepo;
    private final SparePartRepository              sparePartRepo;
    private final UserRepository                   userRepo;
    private final MaintenanceTechnicianService     technicianService;
    private final AuditService                     auditService;
    private final IdGenerator                      idGenerator;

    public WorkOrderService(WorkOrderRepository workOrderRepo,
                            MaintenanceTechnicianRepository techRepo,
                            WorkOrderPartUsageRepository partUsageRepo,
                            MaintenanceReportRepository reportRepo,
                            SparePartRepository sparePartRepo,
                            UserRepository userRepo,
                            MaintenanceTechnicianService technicianService,
                            AuditService auditService,
                            IdGenerator idGenerator) {
        this.workOrderRepo = workOrderRepo;
        this.techRepo = techRepo;
        this.partUsageRepo = partUsageRepo;
        this.reportRepo = reportRepo;
        this.sparePartRepo = sparePartRepo;
        this.userRepo = userRepo;
        this.technicianService = technicianService;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    // ── Create ───────────────────────────────────────────────────────

    /**
     * Creates a new work order.  Any authenticated hospital user may report an issue.
     */
    public Map<String, Object> createWorkOrder(CreateWorkOrderRequest req, Long reporterId) {
        User reporter = userRepo.findById(reporterId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + reporterId));

        WorkOrder wo = new WorkOrder();
        wo.setWorkOrderNumber(idGenerator.generateWorkOrderNumber());
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setDepartment(req.getDepartment());
        wo.setLocation(req.getLocation());
        wo.setPriority(req.getPriority() != null ? req.getPriority() : WorkOrder.Priority.MEDIUM);
        wo.setStatus(WorkOrderStatus.PENDING);
        wo.setReportedBy(reporter);

        WorkOrder saved = workOrderRepo.save(wo);

        auditService.log(reporterId, "CREATE_WORK_ORDER", "WorkOrder", saved.getId(),
                "SYSTEM", saved.getWorkOrderNumber() + " priority=" + saved.getPriority());

        if (saved.getPriority() == WorkOrder.Priority.EMERGENCY) {
            log.warn("🚨 EMERGENCY work order created: {} at {} by {}",
                saved.getWorkOrderNumber(), saved.getLocation(), reporter.getFullName());
        }

        return toWorkOrderMap(saved);
    }

    // ── Assign ───────────────────────────────────────────────────────

    /**
     * Assigns a work order to a technician.
     * Requires MANAGER or ADMIN role (enforced at controller level).
     * For EMERGENCY orders, auto-assignment is also available.
     */
    public Map<String, Object> assignWorkOrder(Long workOrderId, AssignWorkOrderRequest req,
                                               Long managerId) {
        WorkOrder wo = findById(workOrderId);

        if (wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign a " + wo.getStatus() + " work order.");
        }

        MaintenanceTechnician tech = techRepo.findById(req.getTechnicianId())
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + req.getTechnicianId()));

        wo.setAssignedTo(tech);
        wo.setStatus(WorkOrderStatus.ASSIGNED);
        if (req.getEstimatedCost() != null) wo.setEstimatedCost(req.getEstimatedCost());

        workOrderRepo.save(wo);

        auditService.log(managerId, "ASSIGN_WORK_ORDER", "WorkOrder", wo.getId(),
                "SYSTEM", wo.getWorkOrderNumber() + " → " + tech.getTechnicianId());

        log.info("Work order {} assigned to technician {}", wo.getWorkOrderNumber(), tech.getTechnicianId());
        return toWorkOrderMap(wo);
    }

    /**
     * Auto-assigns an EMERGENCY work order to the best available on-call technician.
     */
    public Map<String, Object> autoAssignEmergency(Long workOrderId, Long managerId) {
        WorkOrder wo = findById(workOrderId);
        if (wo.getPriority() != WorkOrder.Priority.EMERGENCY) {
            throw new BadRequestException("Auto-assign is only available for EMERGENCY priority orders.");
        }

        MaintenanceTechnician tech = technicianService.findBestAvailableTechnician(null);
        wo.setAssignedTo(tech);
        wo.setStatus(WorkOrderStatus.ASSIGNED);
        workOrderRepo.save(wo);

        auditService.log(managerId, "AUTO_ASSIGN_EMERGENCY", "WorkOrder", wo.getId(),
                "SYSTEM", wo.getWorkOrderNumber() + " → " + tech.getTechnicianId());

        log.warn("🚨 Emergency work order {} auto-assigned to {}",
            wo.getWorkOrderNumber(), tech.getTechnicianId());

        return toWorkOrderMap(wo);
    }

    // ── Status update ────────────────────────────────────────────────

    /**
     * Updates work order status.
     * Technicians can move their own orders from ASSIGNED → IN_PROGRESS.
     */
    public Map<String, Object> updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest req,
                                            Long userId) {
        WorkOrder wo = findById(workOrderId);

        WorkOrderStatus newStatus;
        try {
            newStatus = WorkOrderStatus.valueOf(req.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + req.getStatus());
        }

        if (wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a " + wo.getStatus() + " work order.");
        }

        wo.setStatus(newStatus);
        if (req.getNotes() != null) wo.setResolutionNotes(req.getNotes());
        workOrderRepo.save(wo);

        auditService.log(userId, "UPDATE_WORK_ORDER_STATUS", "WorkOrder", wo.getId(),
                "SYSTEM", wo.getWorkOrderNumber() + " → " + newStatus);

        return toWorkOrderMap(wo);
    }

    // ── Complete ─────────────────────────────────────────────────────

    /**
     * Completes a work order.  Atomically:
     * <ol>
     *   <li>Records resolution notes and actual cost</li>
     *   <li>Deducts all spare parts used from inventory</li>
     *   <li>Creates a {@link MaintenanceReport} for Finance and compliance</li>
     *   <li>Sets status to COMPLETED with timestamp</li>
     * </ol>
     */
    public Map<String, Object> completeWorkOrder(Long workOrderId, CompleteWorkOrderRequest req,
                                                 String technicianEmail) {
        WorkOrder wo = findById(workOrderId);

        if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new BadRequestException("Work order is already completed.");
        }
        if (wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot complete a cancelled work order.");
        }

        MaintenanceTechnician tech = technicianService.findByEmail(technicianEmail);

        // ── Parts deduction ──────────────────────────────────────────
        if (req.getPartsUsed() != null && !req.getPartsUsed().isEmpty()) {
            for (CompleteWorkOrderRequest.PartUsageItem item : req.getPartsUsed()) {
                SparePart part = sparePartRepo.findByPartCode(item.getPartCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Part not found: " + item.getPartCode()));

                if (part.hasInsufficientStock(item.getQuantity())) {
                    throw new BadRequestException(
                        "Insufficient stock for part " + item.getPartCode() +
                        ": need " + item.getQuantity() + ", have " + part.getStockQuantity());
                }

                part.setStockQuantity(part.getStockQuantity() - item.getQuantity());
                sparePartRepo.save(part);

                WorkOrderPartUsage usage = new WorkOrderPartUsage();
                usage.setWorkOrder(wo);
                usage.setSparePart(part);
                usage.setQuantityUsed(item.getQuantity());
                usage.setCostAtTime(part.getUnitPrice() != null
                    ? part.getUnitPrice() : BigDecimal.ZERO);
                partUsageRepo.save(usage);

                if (part.isLowStock()) {
                    log.warn("LOW STOCK ALERT (spare part): {} ({}) – {} remaining",
                        part.getPartName(), part.getPartCode(), part.getStockQuantity());
                }
            }
        }

        // ── Complete work order ──────────────────────────────────────
        wo.setStatus(WorkOrderStatus.COMPLETED);
        wo.setCompletedDate(LocalDateTime.now());
        wo.setResolutionNotes(req.getResolutionNotes());
        if (req.getActualCost() != null) wo.setActualCost(req.getActualCost());
        workOrderRepo.save(wo);

        // ── Generate maintenance report ──────────────────────────────
        MaintenanceReport report = new MaintenanceReport();
        report.setReportNumber(idGenerator.generateMaintenanceReportNumber());
        report.setWorkOrder(wo);
        report.setTechnician(tech);
        report.setWorkPerformed(req.getResolutionNotes());
        report.setHoursSpent(req.getHoursSpent() != null ? req.getHoursSpent() : BigDecimal.ZERO);
        report.setSignatureRequired(req.isSignatureRequired());
        report.setPatientAreaSignatory(req.getPatientAreaSignatory());
        MaintenanceReport savedReport = reportRepo.save(report);

        auditService.log(tech.getId(), "COMPLETE_WORK_ORDER", "WorkOrder", wo.getId(),
                "SYSTEM", wo.getWorkOrderNumber() + " report=" + savedReport.getReportNumber());

        log.info("Work order {} completed by {} – report {}",
            wo.getWorkOrderNumber(), tech.getTechnicianId(), savedReport.getReportNumber());

        Map<String, Object> result = toWorkOrderMap(wo);
        result.put("reportNumber", savedReport.getReportNumber());
        return result;
    }

    // ── Cancel ───────────────────────────────────────────────────────

    public Map<String, Object> cancelWorkOrder(Long workOrderId, CancelWorkOrderRequest req,
                                               Long userId) {
        WorkOrder wo = findById(workOrderId);

        if (wo.getStatus() == WorkOrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed work order.");
        }
        if (wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BadRequestException("Work order is already cancelled.");
        }

        wo.setStatus(WorkOrderStatus.CANCELLED);
        wo.setResolutionNotes("CANCELLED: " + req.getReason());
        workOrderRepo.save(wo);

        auditService.log(userId, "CANCEL_WORK_ORDER", "WorkOrder", wo.getId(),
                "SYSTEM", "Reason: " + req.getReason());

        Map<String, Object> r = toWorkOrderMap(wo);
        r.put("cancellationReason", req.getReason());
        return r;
    }

    // ── Queries ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkOrder findById(Long id) {
        return workOrderRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Work order not found: " + id));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getWorkOrderById(Long id) {
        WorkOrder wo = findById(id);
        Map<String, Object> result = toWorkOrderMap(wo);

        // Attach parts used
        List<Map<String, Object>> parts = partUsageRepo.findByWorkOrderId(id).stream()
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("partCode", u.getSparePart().getPartCode());
                m.put("partName", u.getSparePart().getPartName());
                m.put("quantityUsed", u.getQuantityUsed());
                m.put("costAtTime", u.getCostAtTime());
                return m;
            }).toList();
        result.put("partsUsed", parts);

        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getByStatus(WorkOrderStatus status) {
        return workOrderRepo.findByStatus(status).stream().map(this::toWorkOrderMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getEmergencyOrders() {
        return workOrderRepo.findActiveEmergencyOrders().stream().map(this::toWorkOrderMap).toList();
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getByTechnician(Long technicianId, Pageable pageable) {
        return workOrderRepo.findAllByAssignedToId(technicianId, pageable).map(this::toWorkOrderMap);
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getByReporter(Long userId, Pageable pageable) {
        return workOrderRepo.findByReportedById(userId, pageable).map(this::toWorkOrderMap);
    }

    // ── Mapper ───────────────────────────────────────────────────────

    public Map<String, Object> toWorkOrderMap(WorkOrder wo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wo.getId());
        m.put("workOrderNumber", wo.getWorkOrderNumber());
        m.put("title", wo.getTitle());
        m.put("description", wo.getDescription());
        m.put("reportedBy", wo.getReportedBy().getFullName());
        m.put("reportedByDepartment", wo.getDepartment());
        m.put("location", wo.getLocation());
        m.put("priority", wo.getPriority());
        m.put("status", wo.getStatus());
        m.put("reportedDate", wo.getReportedDate());
        m.put("completedDate", wo.getCompletedDate());
        m.put("resolutionNotes", wo.getResolutionNotes());
        m.put("estimatedCost", wo.getEstimatedCost());
        m.put("actualCost", wo.getActualCost());
        if (wo.getAssignedTo() != null) {
            m.put("assignedTo", wo.getAssignedTo().getFullName());
            m.put("assignedTechnicianId", wo.getAssignedTo().getTechnicianId());
            m.put("technicianPhone", wo.getAssignedTo().getEmergencyContactPhone());
        } else {
            m.put("assignedTo", null);
        }
        return m;
    }
}
