package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.InsufficientStockException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pharmacy Department – Business Logic Layer.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>View and manage the pending prescriptions queue</li>
 *   <li>Verify prescriptions: allergy check + drug interaction check</li>
 *   <li>Dispense medications and update inventory atomically</li>
 *   <li>Reject prescriptions with documented reasons</li>
 *   <li>Manage medication inventory: receive shipments, adjust stock</li>
 *   <li>Low-stock alerts (future: triggers Purchase Department reorder)</li>
 * </ul>
 *
 * <p><b>Integration contract for other departments:</b></p>
 * <ul>
 *   <li>Doctor:   writes prescriptions → pharmacy reads them</li>
 *   <li>Finance:  reads PrescriptionFulfillment for billing medication costs</li>
 *   <li>Purchase: reads low-stock alerts to generate purchase orders</li>
 *   <li>Storage:  receives incoming shipments registered here</li>
 * </ul>
 */
@Service
@Transactional
public class PharmacyService {

    private final PrescriptionRepository           prescriptionRepo;
    private final PrescriptionFulfillmentRepository fulfillmentRepo;
    private final MedicationInventoryRepository    inventoryRepo;
    private final PatientRepository                patientRepo;
    private final PharmacistRepository             pharmacistRepo;
    private final DrugInteractionService           drugInteractionService;
    private final AuditService                     auditService;

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    public PharmacyService(PrescriptionRepository prescriptionRepo,
                           PrescriptionFulfillmentRepository fulfillmentRepo,
                           MedicationInventoryRepository inventoryRepo,
                           PatientRepository patientRepo,
                           PharmacistRepository pharmacistRepo,
                           DrugInteractionService drugInteractionService,
                           AuditService auditService) {
        this.prescriptionRepo = prescriptionRepo;
        this.fulfillmentRepo = fulfillmentRepo;
        this.inventoryRepo = inventoryRepo;
        this.patientRepo = patientRepo;
        this.pharmacistRepo = pharmacistRepo;
        this.drugInteractionService = drugInteractionService;
        this.auditService = auditService;
    }

    // ════════════════════════════════════════════════════════════════
    // PRESCRIPTION QUEUE
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all ACTIVE prescriptions waiting to be dispensed.
     * This is the pharmacy's primary work queue.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingPrescriptions() {
        return prescriptionRepo
            .findByStatus(Prescription.PrescriptionStatus.ACTIVE)
            .stream()
            .map(this::toPendingPrescriptionMap)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPrescriptionById(Long id) {
        Prescription rx = prescriptionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
        return toPendingPrescriptionMap(rx);
    }

    // ════════════════════════════════════════════════════════════════
    // VERIFICATION
    // ════════════════════════════════════════════════════════════════

    /**
     * Verifies a prescription before dispensing.
     *
     * <p>Checks performed:</p>
     * <ol>
     *   <li>Allergy check against patient's documented allergies</li>
     *   <li>Drug interaction check against the patient's other active medications</li>
     *   <li>Inventory availability check</li>
     * </ol>
     *
     * <p>Returns a verification report.  Pharmacist must acknowledge
     * warnings before dispensing.</p>
     */
    @Transactional(readOnly = true)
    public Map<String, Object> verifyPrescription(Long id) {
        Prescription rx = prescriptionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (rx.getStatus() != Prescription.PrescriptionStatus.ACTIVE) {
            throw new BadRequestException("Prescription is not ACTIVE. Current status: " + rx.getStatus());
        }

        Patient patient = rx.getPatient();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("prescriptionNumber", rx.getPrescriptionNumber());
        report.put("patientId", patient.getPatientId());
        report.put("patientName", patient.getFullName());
        report.put("medication", rx.getMedicationName() + " " + rx.getDosage());
        report.put("prescribedBy", "Dr. " + rx.getDoctor().getFullName());

        // ── 1. Allergy check ───────────────────────────────────────
        List<String> allergyWarnings = new ArrayList<>();
        if (patient.getAllergies() != null && !patient.getAllergies().isBlank()) {
            for (String allergy : patient.getAllergies().split(",")) {
                String a = allergy.trim();
                if (rx.getMedicationName().toLowerCase().contains(a.toLowerCase())) {
                    allergyWarnings.add("⚠ ALLERGY ALERT: Patient is allergic to " + a);
                    log.warn("ALLERGY CONFLICT: {} for patient {}", a, patient.getPatientId());
                }
            }
        }
        report.put("allergyWarnings", allergyWarnings);
        report.put("allergyConflict", !allergyWarnings.isEmpty());
        report.put("patientAllergies", patient.getAllergies());

        // ── 2. Drug interaction check ──────────────────────────────
        List<String> existingMedNames = prescriptionRepo
            .findByPatientAndStatus(patient, Prescription.PrescriptionStatus.ACTIVE)
            .stream()
            .filter(p -> !p.getId().equals(id))   // exclude the current prescription
            .map(Prescription::getMedicationName)
            .collect(Collectors.toList());

        List<DrugInteractionService.Interaction> interactions =
            drugInteractionService.checkInteractions(rx.getMedicationName(), existingMedNames);

        List<Map<String, String>> interactionDetails = interactions.stream()
            .map(i -> Map.of(
                "severity", i.severity(),
                "description", i.description(),
                "clinicalEffect", i.clinicalEffect()
            )).collect(Collectors.toList());

        report.put("drugInteractions", interactionDetails);
        report.put("hasCriticalInteraction", !interactions.isEmpty());
        report.put("existingMedications", existingMedNames);

        // ── 3. Inventory check ─────────────────────────────────────
        Optional<MedicationInventory> stock =
            inventoryRepo.findByMedicationCode(rx.getMedicationCode());

        if (stock.isEmpty()) {
            report.put("inventoryAvailable", false);
            report.put("inventoryErrorCode", "MEDICATION_NOT_FOUND");
        } else {
            MedicationInventory inv = stock.get();
            boolean sufficient = inv.getCurrentStock() >= rx.getQuantity();
            report.put("inventoryAvailable", sufficient);
            report.put("currentStock", inv.getCurrentStock());
            report.put("requiredQuantity", rx.getQuantity());
            report.put("shelfLocation", inv.getLocationShelf());
        }

        // ── Summary ────────────────────────────────────────────────
        boolean safeToDispense = allergyWarnings.isEmpty()
            && interactions.isEmpty()
            && stock.isPresent()
            && stock.get().getCurrentStock() >= rx.getQuantity();

        report.put("verificationStatus", safeToDispense ? "PASSED" : "WARNINGS_PRESENT");
        report.put("safeToDispense", safeToDispense);
        report.put("message", safeToDispense
            ? "All checks passed. Ready to dispense."
            : "Verification flagged issues. Review warnings before dispensing.");

        return report;
    }

    // ════════════════════════════════════════════════════════════════
    // DISPENSING
    // ════════════════════════════════════════════════════════════════

    /**
     * Dispenses a prescription.
     *
     * <p>Atomically:</p>
     * <ol>
     *   <li>Checks stock is sufficient</li>
     *   <li>Decrements inventory</li>
     *   <li>Creates a PrescriptionFulfillment record</li>
     *   <li>Updates prescription status to DISPENSED</li>
     *   <li>Writes audit log entry</li>
     * </ol>
     */
    public Map<String, Object> dispensePrescription(Long id, DispenseRequest req, String pharmacistEmail) {
        Pharmacist pharmacist = getPharmacistByEmail(pharmacistEmail);
        Prescription rx = prescriptionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (rx.getStatus() != Prescription.PrescriptionStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE prescriptions can be dispensed. Status: " + rx.getStatus());
        }

        // ── Inventory check and decrement ──────────────────────────
        MedicationInventory inventory = inventoryRepo.findByMedicationCode(rx.getMedicationCode())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Medication not in inventory: " + rx.getMedicationCode()));

        int qtyToDispense = req.getQuantityDispensed() > 0
            ? req.getQuantityDispensed() : rx.getQuantity();

        if (inventory.hasInsufficientStock(qtyToDispense)) {
            throw new InsufficientStockException(
                rx.getMedicationName(), qtyToDispense, inventory.getCurrentStock());
        }

        int stockBefore = inventory.getCurrentStock();
        inventory.setCurrentStock(stockBefore - qtyToDispense);
        inventoryRepo.save(inventory);

        // ── Fulfillment record ─────────────────────────────────────
        PrescriptionFulfillment fulfillment = new PrescriptionFulfillment();
        fulfillment.setPrescription(rx);
        fulfillment.setPharmacist(pharmacist);
        fulfillment.setQuantityDispensed(qtyToDispense);
        fulfillment.setStatus(PrescriptionFulfillment.FulfillmentStatus.COMPLETED);
        fulfillment.setNotes(req.getNotes());
        PrescriptionFulfillment saved = fulfillmentRepo.save(fulfillment);

        // ── Update prescription status ─────────────────────────────
        rx.setStatus(Prescription.PrescriptionStatus.DISPENSED);
        prescriptionRepo.save(rx);

        auditService.log(pharmacist.getId(), AuditService.DISPENSE_MEDICATION,
            "Prescription", rx.getId(), "SYSTEM",
            rx.getPrescriptionNumber() + " qty=" + qtyToDispense +
            " stockBefore=" + stockBefore + " stockAfter=" + inventory.getCurrentStock());

        log.info("Prescription {} dispensed by {} – stock {} → {}",
            rx.getPrescriptionNumber(), pharmacist.getPharmacistId(),
            stockBefore, inventory.getCurrentStock());

        // ── Low-stock alert ────────────────────────────────────────
        // Purchase Department will act on this in Phase 2
        if (inventory.isLowStock()) {
            log.warn("LOW STOCK ALERT: {} ({}) – {} units remaining (reorder level: {})",
                inventory.getMedicationName(), inventory.getMedicationCode(),
                inventory.getCurrentStock(), inventory.getReorderLevel());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fulfillmentId", saved.getId());
        response.put("prescriptionNumber", rx.getPrescriptionNumber());
        response.put("status", "DISPENSED");
        response.put("quantityDispensed", qtyToDispense);
        response.put("remainingStock", inventory.getCurrentStock());
        response.put("lowStockAlert", inventory.isLowStock());
        response.put("message", "Prescription dispensed successfully. Ready for patient pickup.");
        return response;
    }

    /**
     * Rejects a prescription with a documented reason.
     * Rejected prescriptions remain in the database for audit purposes.
     */
    public Map<String, Object> rejectPrescription(Long id, RejectPrescriptionRequest req,
                                                   String pharmacistEmail) {
        Pharmacist pharmacist = getPharmacistByEmail(pharmacistEmail);
        Prescription rx = prescriptionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (rx.getStatus() != Prescription.PrescriptionStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE prescriptions can be rejected.");
        }

        // Create a REJECTED fulfillment record (for audit trail)
        PrescriptionFulfillment rejection = new PrescriptionFulfillment();
        rejection.setPrescription(rx);
        rejection.setPharmacist(pharmacist);
        rejection.setQuantityDispensed(0);
        rejection.setStatus(PrescriptionFulfillment.FulfillmentStatus.REJECTED);
        rejection.setRejectionReason(req.getRejectionReason());
        fulfillmentRepo.save(rejection);

        // Prescription reverts to CANCELLED so doctor is notified
        rx.setStatus(Prescription.PrescriptionStatus.CANCELLED);
        prescriptionRepo.save(rx);

        auditService.log(pharmacist.getId(), AuditService.REJECT_PRESCRIPTION,
            "Prescription", rx.getId(), "SYSTEM",
            "Reason: " + req.getRejectionReason());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prescriptionNumber", rx.getPrescriptionNumber());
        response.put("status", "REJECTED");
        response.put("reason", req.getRejectionReason());
        response.put("message", "Prescription rejected. Doctor has been notified.");
        return response;
    }

    // ════════════════════════════════════════════════════════════════
    // INVENTORY MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllInventory() {
        return inventoryRepo.findAll().stream().map(this::toInventoryMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLowStockItems() {
        return inventoryRepo.findLowStockItems().stream().map(this::toInventoryMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInventoryByCode(String code) {
        MedicationInventory inv = inventoryRepo.findByMedicationCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Medication not found: " + code));
        return toInventoryMap(inv);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchMedications(String term) {
        return inventoryRepo.searchByNameOrCode(term).stream().map(this::toInventoryMap).toList();
    }

    /**
     * Records receipt of a new shipment and updates stock.
     *
     * <p>This method is also the integration hook for the Storage and
     * Purchase departments: when a purchase order is fulfilled, those
     * departments call this endpoint to update pharmacy stock levels.</p>
     */
    public Map<String, Object> receiveShipment(ReceiveShipmentRequest req, String pharmacistEmail) {
        Pharmacist pharmacist = getPharmacistByEmail(pharmacistEmail);
        MedicationInventory inv = inventoryRepo.findByMedicationCode(req.getMedicationCode())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Medication not found in inventory: " + req.getMedicationCode()));

        int before = inv.getCurrentStock();
        inv.setCurrentStock(before + req.getQuantity());
        inventoryRepo.save(inv);

        auditService.log(pharmacist.getId(), AuditService.RECEIVE_SHIPMENT,
            "MedicationInventory", inv.getId(), "SYSTEM",
            inv.getMedicationCode() + " +" + req.getQuantity() +
            " stockBefore=" + before + " stockAfter=" + inv.getCurrentStock() +
            (req.getBatchNumber() != null ? " batch=" + req.getBatchNumber() : ""));

        log.info("Shipment received: {} × {} – stock {} → {}",
            req.getQuantity(), inv.getMedicationName(), before, inv.getCurrentStock());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("medicationCode", inv.getMedicationCode());
        response.put("medicationName", inv.getMedicationName());
        response.put("quantityReceived", req.getQuantity());
        response.put("previousStock", before);
        response.put("newStock", inv.getCurrentStock());
        response.put("message", "Shipment recorded successfully.");
        return response;
    }

    /**
     * Manual stock adjustment (damage, expiry, loss).
     * Positive adjustment = add stock. Negative = remove stock.
     */
    public Map<String, Object> adjustStock(AdjustStockRequest req, String pharmacistEmail) {
        Pharmacist pharmacist = getPharmacistByEmail(pharmacistEmail);
        MedicationInventory inv = inventoryRepo.findByMedicationCode(req.getMedicationCode())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Medication not found: " + req.getMedicationCode()));

        int before = inv.getCurrentStock();
        int newStock = before + req.getAdjustment();

        if (newStock < 0) {
            throw new BadRequestException(
                "Adjustment would result in negative stock (" + newStock + ")");
        }

        inv.setCurrentStock(newStock);
        inventoryRepo.save(inv);

        auditService.log(pharmacist.getId(), AuditService.ADJUST_STOCK,
            "MedicationInventory", inv.getId(), "SYSTEM",
            inv.getMedicationCode() + " adj=" + req.getAdjustment() +
            " reason=" + req.getReason() + " before=" + before + " after=" + newStock);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("medicationCode", inv.getMedicationCode());
        response.put("adjustment", req.getAdjustment());
        response.put("reason", req.getReason());
        response.put("previousStock", before);
        response.put("newStock", newStock);
        response.put("message", "Stock adjusted successfully.");
        return response;
    }

    // ════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════

    private Pharmacist getPharmacistByEmail(String email) {
        return pharmacistRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Pharmacist not found: " + email));
    }

    private Map<String, Object> toPendingPrescriptionMap(Prescription rx) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", rx.getId());
        m.put("prescriptionNumber", rx.getPrescriptionNumber());
        m.put("patientId", rx.getPatient().getPatientId());
        m.put("patientName", rx.getPatient().getFullName());
        m.put("patientAllergies", rx.getPatient().getAllergies());
        m.put("prescribedBy", "Dr. " + rx.getDoctor().getFullName());
        m.put("doctorSpecialization", rx.getDoctor().getSpecialization());
        m.put("medicationName", rx.getMedicationName() + " " + rx.getDosage());
        m.put("medicationCode", rx.getMedicationCode());
        m.put("frequency", rx.getFrequency());
        m.put("quantity", rx.getQuantity());
        m.put("duration", rx.getDuration() + " days");
        m.put("instructions", rx.getInstructions());
        m.put("isControlledSubstance", rx.isControlledSubstance());
        m.put("status", rx.getStatus());
        m.put("prescribedDate", rx.getPrescribedDate());

        // Inline stock availability
        inventoryRepo.findByMedicationCode(rx.getMedicationCode()).ifPresent(inv -> {
            m.put("inventoryAvailable", inv.getCurrentStock() >= rx.getQuantity());
            m.put("currentStock", inv.getCurrentStock());
        });

        return m;
    }

    private Map<String, Object> toInventoryMap(MedicationInventory inv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", inv.getId());
        m.put("medicationCode", inv.getMedicationCode());
        m.put("medicationName", inv.getMedicationName());
        m.put("genericName", inv.getGenericName());
        m.put("strength", inv.getStrength());
        m.put("form", inv.getForm());
        m.put("manufacturer", inv.getManufacturer());
        m.put("currentStock", inv.getCurrentStock());
        m.put("reorderLevel", inv.getReorderLevel());
        m.put("isLowStock", inv.isLowStock());
        m.put("unitPrice", inv.getUnitPrice());
        m.put("locationShelf", inv.getLocationShelf());
        m.put("requiresPrescription", inv.isRequiresPrescription());
        m.put("lastUpdated", inv.getLastUpdated());
        return m;
    }
}
