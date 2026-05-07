package com.hospital.controller;

import com.hospital.dto.RegisterTechnicianRequest;
import com.hospital.service.MaintenanceTechnicianService;
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
 * Maintenance Technician Management Controller.
 *
 * <p>Base path: {@code /api/maintenance/technicians}</p>
 *
 * <pre>
 * POST /register           – Register new technician        [ADMIN, MANAGEMENT]
 * GET  /                   – List all technicians (paged)   [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /{id}               – Get technician by ID           [ADMIN, MANAGEMENT, MAINTENANCE]
 * PUT  /{id}/on-call       – Toggle on-call status          [ADMIN, MANAGEMENT]
 * GET  /on-call            – Get on-call technicians        [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /by-specialization  – Filter by specialization       [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /dashboard          – Personal dashboard (self)      [MAINTENANCE]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/technicians")
public class MaintenanceTechnicianController {

    private final MaintenanceTechnicianService technicianService;

    // Explicit constructor to avoid Lombok/annotation-processor dependence in IDEs
    public MaintenanceTechnicianController(MaintenanceTechnicianService technicianService) {
        this.technicianService = technicianService;
    }

    /** POST /api/maintenance/technicians/register
     *  Registers a new maintenance technician account. */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody RegisterTechnicianRequest request) {
        return ResponseEntity.ok(technicianService.registerTechnician(request));
    }

    /** GET /api/maintenance/technicians
     *  Returns a paginated list of all technicians. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<?> getAll(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(technicianService.getAllTechnicians(pageable));
    }

    /** GET /api/maintenance/technicians/{id} */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(technicianService.getTechnicianById(id));
    }

    /** PUT /api/maintenance/technicians/{id}/on-call?status=true
     *  Sets or clears the on-call flag for shift coverage. */
    @PutMapping("/{id}/on-call")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT')")
    public ResponseEntity<Map<String, Object>> updateOnCall(
            @PathVariable Long id,
            @RequestParam boolean status) {
        return ResponseEntity.ok(technicianService.updateOnCallStatus(id, status));
    }

    /** GET /api/maintenance/technicians/on-call
     *  Returns all technicians currently flagged as on-call. */
    @GetMapping("/on-call")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<List<Map<String, Object>>> getOnCall() {
        return ResponseEntity.ok(technicianService.getOnCallTechnicians());
    }

    /** GET /api/maintenance/technicians/by-specialization?spec=Electrical
     *  Filters technicians by area of expertise. */
    @GetMapping("/by-specialization")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<List<Map<String, Object>>> bySpecialization(@RequestParam String spec) {
        return ResponseEntity.ok(technicianService.getBySpecialization(spec));
    }

    /** GET /api/maintenance/technicians/dashboard
     *  Returns the personal dashboard for the currently logged-in technician. */
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> myDashboard(Authentication auth) {
        Map<String, Object> self = technicianService.toTechnicianMap(
            technicianService.findByEmail(auth.getName()));
        return ResponseEntity.ok(self);
    }
}
