package com.hospital.controller;

import com.hospital.dto.CreateSparePartRequest;
import com.hospital.dto.UpdateStockRequest;
import com.hospital.repository.UserRepository;
import com.hospital.service.SparePartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Spare Part Inventory Controller.
 *
 * <p>Base path: {@code /api/maintenance/spare-parts}</p>
 *
 * <pre>
 * POST /                  – Add new spare part          [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /                  – List all parts              [ADMIN, MANAGEMENT, MAINTENANCE, FINANCE]
 * GET  /{id}              – Get part detail             [ADMIN, MANAGEMENT, MAINTENANCE, FINANCE]
 * PUT  /{id}/stock        – Receive / adjust stock      [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /low-stock         – Low stock alert list        [ADMIN, MANAGEMENT, MAINTENANCE, FINANCE, PURCHASE]
 * POST /{id}/deduct       – Deduct stock for job        [ADMIN, MANAGEMENT, MAINTENANCE]
 * GET  /search            – Search by keyword           [ADMIN, MANAGEMENT, MAINTENANCE, FINANCE]
 * </pre>
 */
@RestController
@RequestMapping("/api/maintenance/spare-parts")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE', 'FINANCE')")
public class SparePartController {

    private final SparePartService sparePartService;
    private final UserRepository   userRepo;

    // Explicit constructor to ensure fields are initialized without Lombok
    public SparePartController(SparePartService sparePartService, UserRepository userRepo) {
        this.sparePartService = sparePartService;
        this.userRepo = userRepo;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> add(
            @Valid @RequestBody CreateSparePartRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(sparePartService.addSparePart(request, userId));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(sparePartService.getAllParts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sparePartService.getPartById(id));
    }

    /** PUT /api/maintenance/spare-parts/{id}/stock
     *  Adjusts stock. Use positive quantity to receive a shipment,
     *  negative to remove damaged/expired units.
     *
     *  <p>Example – receive 50 units:</p>
     *  <pre>{ "quantity": 50, "reason": "Received from supplier XYZ" }</pre>
     *
     *  <p>Example – remove 3 damaged:</p>
     *  <pre>{ "quantity": -3, "reason": "Water damage – batch B2024" }</pre>
     */
    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> updateStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest request,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(sparePartService.updateStock(id, request, userId));
    }

    /** GET /api/maintenance/spare-parts/low-stock
     *  Lists parts at or below reorder threshold.
     *  Purchase Department monitors this endpoint to trigger purchase orders. */
    @GetMapping("/low-stock")
    public ResponseEntity<List<Map<String, Object>>> lowStock() {
        return ResponseEntity.ok(sparePartService.getLowStockParts());
    }

    /** POST /api/maintenance/spare-parts/{id}/deduct?quantity=2&workOrderId=15
     *  Deducts units for a specific work order (ad-hoc; normally done via complete-work-order). */
    @PostMapping("/{id}/deduct")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGEMENT', 'MAINTENANCE')")
    public ResponseEntity<Map<String, Object>> deduct(
            @PathVariable Long id,
            @RequestParam int quantity,
            @RequestParam(required = false) Long workOrderId,
            Authentication auth) {
        Long userId = resolveUserId(auth);
        sparePartService.deductStock(id, quantity, workOrderId, userId);
        return ResponseEntity.ok(sparePartService.getPartById(id));
    }

    /** GET /api/maintenance/spare-parts/search?q=HVAC
     *  Searches by part name, code, or category. */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam String q) {
        return ResponseEntity.ok(sparePartService.searchParts(q));
    }

    private Long resolveUserId(Authentication auth) {
        return userRepo.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }
}
