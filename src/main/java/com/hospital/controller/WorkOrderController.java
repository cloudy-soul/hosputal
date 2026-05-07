package com.hospital.controller;

import com.hospital.dto.CreateWorkOrderRequest;
import com.hospital.dto.AssignWorkOrderRequest;
import com.hospital.dto.UpdateWorkOrderStatusRequest;
import com.hospital.dto.CompleteWorkOrderRequest;
import com.hospital.dto.CancelWorkOrderRequest;
import com.hospital.entity.WorkOrder.WorkOrderStatus;
import com.hospital.repository.UserRepository;
import com.hospital.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Work Order Controller – the core of the Maintenance Department API.
 *
 * <p>Base path: {@code /api/maintenance/work-orders}</p>
 *
 * <pre>
 * POST /                  – Create work order       [any authenticated user]
 * GET  /                  – List by status filter   [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /{id}              – Get full detail         [ADMIN, MANAGEMENT, MAINTENANCE]
 * PUT  /{id}/assign       – Assign technician       [ADMIN, MANAGEMENT]
 * PUT  /{id}/assign/auto  – Auto-assign emergency   [ADMIN, MANAGEMENT]
 * PUT  /{id}/status       – Update status           [ADMIN, MANAGEMENT, MAINTENANCE]
 * PUT  /{id}/complete     – Complete + report       [MAINTENANCE]
 * DELETE /{id}/cancel     – Cancel order            [ADMIN, MANAGEMENT]
 * GET  /emergency         – Active emergencies      [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /my-orders         – Technician's own orders [MAINTENANCE]
 * GET  /my-reports        – Reporter's own orders   [any authenticated user]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final UserRepository   userRepo;

    // Explicit constructor to avoid Lombok annotation-processor dependency in IDE
    public WorkOrderController(WorkOrderService workOrderService, UserRepository userRepo) {
        this.workOrderService = workOrderService;
        this.userRepo = userRepo;
    }

    /** POST /api/maintenance/work-orders
     *  Any authenticated hospital employee can report a maintenance issue.
     *
     *  <p>Example 
     * request body:</p>
     *  <pre>
     *  {
     *    "title": "Air conditioning unit not cooling",
     *    "description": "AC in pharmacy dispensing room making loud noise",
     *    "department": "Pharmacy",
     *    "location": "Room 104 – Dispensing",
     *    "priority": "HIGH"
     *  }
     *  </pre>
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @Valid @RequestBody CreateWorkOrderRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.createWorkOrder(request, userId));
    }

    /** GET /api/maintenance/work-orders?status=PENDING
     *  Lists work orders filtered by status. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<List<Map<String, Object>>> list(
            @RequestParam(required = false, defaultValue = "PENDING") String status) {
        WorkOrderStatus s;
        try {
            s = WorkOrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            s = WorkOrderStatus.PENDING;
        }
        return ResponseEntity.ok(workOrderService.getByStatus(s));
    }

    /** GET /api/maintenance/work-orders/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(workOrderService.getWorkOrderById(id));
    }

    /** PUT /api/maintenance/work-orders/{id}/assign
     *  Assigns a technician and optionally sets an estimated cost.
     *
     *  <p>Example request body:</p>
     *  <pre>
     *  { "technicianId": 5, "estimatedCost": 150.00 }
     *  </pre>
     */
    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignWorkOrderRequest request,
            Authentication auth) {
        Long managerId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.assignWorkOrder(id, request, managerId));
    }

    /** PUT /api/maintenance/work-orders/{id}/assign/auto
     *  Auto-assigns an EMERGENCY work order to the best on-call technician. */
    @PutMapping("/{id}/assign/auto")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> autoAssign(
            @PathVariable Long id,
            Authentication auth) {
        Long managerId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.autoAssignEmergency(id, managerId));
    }

    /** PUT /api/maintenance/work-orders/{id}/status
     *  Updates work order status. Technicians use this to move
     *  ASSIGNED → IN_PROGRESS.
     *
     *  <p>Example request body:</p>
     *  <pre>
     *  { "status": "IN_PROGRESS", "notes": "Started disassembly of AC unit" }
     *  </pre>
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkOrderStatusRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.updateStatus(id, request, userId));
    }

    /** PUT /api/maintenance/work-orders/{id}/complete
     *  Completes the work order. Creates a maintenance report automatically.
     *  Records parts used and decrements spare-part inventory atomically.
     *
     *  <p>Example request body:</p>
     *  <pre>
     *  {
     *    "resolutionNotes": "Replaced compressor capacitor. Unit cooling normally.",
     *    "actualCost": 220.50,
     *    "hoursSpent": 3.5,
     *    "partsUsed": [
     *      { "partCode": "SP-001", "quantity": 1 }
     *    ]
     *  }
     *  </pre>
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long id,
            @Valid @RequestBody CompleteWorkOrderRequest request,
            Authentication auth) {
        return ResponseEntity.ok(workOrderService.completeWorkOrder(id, request, auth.getName()));
    }

    /** DELETE /api/maintenance/work-orders/{id}/cancel
     *  Cancels a work order with a documented reason. */
    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelWorkOrderRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.cancelWorkOrder(id, request, userId));
    }

    /** GET /api/maintenance/work-orders/emergency
     *  Returns all active EMERGENCY priority work orders. */
    @GetMapping("/emergency")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<List<Map<String, Object>>> getEmergencies() {
        return ResponseEntity.ok(workOrderService.getEmergencyOrders());
    }

    /** GET /api/maintenance/work-orders/my-orders
     *  Returns all work orders assigned to the logged-in technician. */
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<?> myOrders(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        Long techId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.getByTechnician(techId, pageable));
    }

    /** GET /api/maintenance/work-orders/my-reports
     *  Returns work orders submitted by the currently authenticated user. */
    @GetMapping("/my-reports")
    public ResponseEntity<?> myReports(
            Authentication auth,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(workOrderService.getByReporter(userId, pageable));
    }

    private Long resolveUserId(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
