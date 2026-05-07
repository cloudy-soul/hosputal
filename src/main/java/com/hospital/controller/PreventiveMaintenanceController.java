package com.hospital.controller;

import com.hospital.dto.*;
import com.hospital.repository.UserRepository;
import com.hospital.service.PreventiveMaintenanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Preventive Maintenance Controller.
 *
 * <p>Base path: {@code /api/maintenance/preventive}</p>
 *
 * <pre>
 * POST /tasks              – Create preventive task    [ADMIN, MANAGEMENT]
 * GET  /tasks              – List all tasks            [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /tasks/overdue      – Get overdue tasks         [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /tasks/{id}         – Get task detail           [ADMIN, MANAGEMENT, MAINTENANCE]
 * PUT  /tasks/{id}/complete – Complete task, advance date [MAINTENANCE]
 * PUT  /tasks/{id}/assign  – Assign to technician      [ADMIN, MANAGEMENT]
 * GET  /schedule/monthly   – Monthly schedule          [ADMIN, MANAGEMENT, MAINTENANCE]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/preventive")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
public class PreventiveMaintenanceController {

    private final PreventiveMaintenanceService preventiveService;
    private final UserRepository               userRepo;

    // Explicit constructor to avoid Lombok annotation-processing issues in the IDE
    public PreventiveMaintenanceController(PreventiveMaintenanceService preventiveService,
                                           UserRepository userRepo) {
        this.preventiveService = preventiveService;
        this.userRepo = userRepo;
    }

    @PostMapping("/tasks")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> createTask(
            @Valid @RequestBody CreatePreventiveTaskRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(preventiveService.createTask(request, userId));
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> listTasks() {
        return ResponseEntity.ok(preventiveService.getAllTasks());
    }

    @GetMapping("/tasks/overdue")
    public ResponseEntity<List<Map<String, Object>>> getOverdue() {
        return ResponseEntity.ok(preventiveService.getOverdueTasks());
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<Map<String, Object>> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(preventiveService.getTaskById(id));
    }

    /** PUT /api/maintenance/preventive/tasks/{id}/complete
     *  Marks the task done and auto-advances nextDueDate by frequency. */
    @PutMapping("/tasks/{id}/complete")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable Long id,
            @Valid @RequestBody CompleteTaskRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(preventiveService.completeTask(id, request, userId));
    }

    /** PUT /api/maintenance/preventive/tasks/{id}/assign?technicianId=3 */
    @PutMapping("/tasks/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> assignTask(
            @PathVariable Long id,
            @RequestParam Long technicianId,
            Authentication auth) {
        Long managerId = resolveUserId(auth);
        return ResponseEntity.ok(preventiveService.assignTaskToTechnician(id, technicianId, managerId));
    }

    /** GET /api/maintenance/preventive/schedule/monthly?year=2025&month=4
     *  Returns all tasks due in the given month with overdue flags. */
    @GetMapping("/schedule/monthly")
    public ResponseEntity<List<Map<String, Object>>> monthlySchedule(
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(preventiveService.getMonthlySchedule(year, month));
    }

    private Long resolveUserId(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
