package com.hospital.controller;

import com.hospital.dto.*;
import com.hospital.service.PharmacyService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Pharmacy Department REST Controller.
 *
 * <p>All endpoints require the PHARMACIST role.
 * The pharmacy queue is driven by prescriptions written by the Doctor
 * Department – no additional plumbing required between the two.</p>
 *
 * <p>Base URL: /api/pharmacy</p>
 *
 * <pre>
 * GET  /api/pharmacy/prescriptions/pending       – Pending queue
 * GET  /api/pharmacy/prescriptions/{id}          – Prescription detail
 * POST /api/pharmacy/prescriptions/{id}/verify   – Verify (allergy + interaction + stock)
 * POST /api/pharmacy/prescriptions/{id}/dispense – Dispense + deduct stock
 * POST /api/pharmacy/prescriptions/{id}/reject   – Reject with reason
 * GET  /api/pharmacy/inventory                   – Full stock list
 * GET  /api/pharmacy/inventory/low-stock         – Low stock alerts
 * GET  /api/pharmacy/inventory/{code}            – Single medication
 * POST /api/pharmacy/inventory/receive           – Receive new shipment
 * PUT  /api/pharmacy/inventory/adjust            – Manual stock adjustment
 * GET  /api/pharmacy/medications/search          – Search medications
 * </pre>
 */
@RestController
@RequestMapping("/api/pharmacy")
@PreAuthorize("hasRole('PHARMACIST')")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

   

    // ── Prescription Queue ───────────────────────────────────────────

    /** GET /api/pharmacy/prescriptions/pending
     *  Returns all ACTIVE prescriptions written by doctors.
     *  This is the pharmacist's primary worklist. */
    @GetMapping("/prescriptions/pending")
    public ResponseEntity<List<Map<String, Object>>> getPendingPrescriptions() {
        return ResponseEntity.ok(pharmacyService.getPendingPrescriptions());
    }

    /** GET /api/pharmacy/prescriptions/{id}
     *  Returns full detail for a single prescription including patient allergies. */
    @GetMapping("/prescriptions/{id}")
    public ResponseEntity<Map<String, Object>> getPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyService.getPrescriptionById(id));
    }

    /** POST /api/pharmacy/prescriptions/{id}/verify
     *  Runs all safety checks before dispensing:
     *  <ol>
     *    <li>Allergy check against patient record</li>
     *    <li>Drug interaction check against existing active medications</li>
     *    <li>Inventory availability check</li>
     *  </ol>
     *  Returns a verification report.  The pharmacist must review any
     *  warnings before proceeding to dispense. */
    @PostMapping("/prescriptions/{id}/verify")
    public ResponseEntity<Map<String, Object>> verifyPrescription(@PathVariable Long id) {
        return ResponseEntity.ok(pharmacyService.verifyPrescription(id));
    }

    /** POST /api/pharmacy/prescriptions/{id}/dispense
     *  Dispenses the medication.  Atomically:
     *  - Decrements inventory stock
     *  - Creates a PrescriptionFulfillment record
     *  - Updates prescription status to DISPENSED
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "quantityDispensed": 30,
     *    "notes": "Counselled patient on morning dosing"
     *  }
     *  </pre>
     */
    @PostMapping("/prescriptions/{id}/dispense")
    public ResponseEntity<Map<String, Object>> dispensePrescription(
            @PathVariable Long id,
            @Valid @RequestBody DispenseRequest request,
            Authentication auth) {
        return ResponseEntity.ok(pharmacyService.dispensePrescription(id, request, auth.getName()));
    }

    /** POST /api/pharmacy/prescriptions/{id}/reject
     *  Rejects a prescription with a documented reason.
     *  Prescription is marked CANCELLED; the doctor is notified via status change.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "rejectionReason": "Patient has documented allergy to Penicillin"
     *  }
     *  </pre>
     */
    @PostMapping("/prescriptions/{id}/reject")
    public ResponseEntity<Map<String, Object>> rejectPrescription(
            @PathVariable Long id,
            @Valid @RequestBody RejectPrescriptionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(pharmacyService.rejectPrescription(id, request, auth.getName()));
    }

    // ── Inventory ────────────────────────────────────────────────────

    /** GET /api/pharmacy/inventory
     *  Returns the complete medication inventory with stock levels. */
    @GetMapping("/inventory")
    public ResponseEntity<List<Map<String, Object>>> getAllInventory() {
        return ResponseEntity.ok(pharmacyService.getAllInventory());
    }

    /** GET /api/pharmacy/inventory/low-stock
     *  Returns medications at or below reorder threshold.
     *  The Purchase Department (Phase 2) will monitor this endpoint
     *  to trigger automated purchase orders. */
    @GetMapping("/inventory/low-stock")
    public ResponseEntity<List<Map<String, Object>>> getLowStockItems() {
        return ResponseEntity.ok(pharmacyService.getLowStockItems());
    }

    /** GET /api/pharmacy/inventory/{code}
     *  Returns a single medication by its RxNorm/NDC code. */
    @GetMapping("/inventory/{code}")
    public ResponseEntity<Map<String, Object>> getInventoryByCode(@PathVariable String code) {
        return ResponseEntity.ok(pharmacyService.getInventoryByCode(code));
    }

    /** POST /api/pharmacy/inventory/receive
     *  Records receipt of a new medication shipment and updates stock.
     *  Integration hook for Storage and Purchase departments.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "medicationCode": "0003",
     *    "quantity": 200,
     *    "batchNumber": "BATCH-2024-0042",
     *    "expiryDate": "2026-06-01",
     *    "notes": "Received from supplier ABC Pharma"
     *  }
     *  </pre>
     */
    @PostMapping("/inventory/receive")
    public ResponseEntity<Map<String, Object>> receiveShipment(
            @Valid @RequestBody ReceiveShipmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(pharmacyService.receiveShipment(request, auth.getName()));
    }

    /** PUT /api/pharmacy/inventory/adjust
     *  Manually adjusts stock for damage, expiry, or loss.
     *  Positive adjustment adds stock; negative removes stock.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "medicationCode": "0003",
     *    "adjustment": -10,
     *    "reason": "10 units expired – batch EXP-2024-01"
     *  }
     *  </pre>
     */
    @PutMapping("/inventory/adjust")
    public ResponseEntity<Map<String, Object>> adjustStock(
            @Valid @RequestBody AdjustStockRequest request,
            Authentication auth) {
        return ResponseEntity.ok(pharmacyService.adjustStock(request, auth.getName()));
    }

    /** GET /api/pharmacy/medications/search?term=amoxicillin
     *  Searches the inventory by medication name, generic name, or code. */
    @GetMapping("/medications/search")
    public ResponseEntity<List<Map<String, Object>>> searchMedications(@RequestParam String term) {
        return ResponseEntity.ok(pharmacyService.searchMedications(term));
    }
}
