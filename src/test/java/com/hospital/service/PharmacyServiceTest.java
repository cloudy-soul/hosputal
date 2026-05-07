package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.InsufficientStockException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Pharmacy Department service layer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PharmacyService Unit Tests")
class PharmacyServiceTest {

    @Mock PrescriptionRepository            prescriptionRepo;
    @Mock PrescriptionFulfillmentRepository fulfillmentRepo;
    @Mock MedicationInventoryRepository     inventoryRepo;
    @Mock PatientRepository                 patientRepo;
    @Mock PharmacistRepository              pharmacistRepo;
    @Mock DrugInteractionService            drugInteractionService;
    @Mock AuditService                      auditService;

    @InjectMocks PharmacyService pharmacyService;

    // ── Fixtures ─────────────────────────────────────────────────────

    Pharmacist pharmacist;
    Patient    patient;
    Patient    patientWithPenicillinAllergy;
    Prescription activePrescription;
    MedicationInventory lisinoprilStock;

    @BeforeEach
    void setUp() {
        pharmacist = new Pharmacist();
        pharmacist.setId(50L);
        pharmacist.setEmail("john.smith@hospital.com");
        pharmacist.setPharmacistId("PHA-2024-0001");
        pharmacist.setFirstName("John");
        pharmacist.setLastName("Smith");

        patient = new Patient();
        patient.setId(10L);
        patient.setPatientId("PAT-2024-0001");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setAllergies(null);

        patientWithPenicillinAllergy = new Patient();
        patientWithPenicillinAllergy.setId(11L);
        patientWithPenicillinAllergy.setPatientId("PAT-2024-0002");
        patientWithPenicillinAllergy.setFirstName("Jane");
        patientWithPenicillinAllergy.setLastName("Smith");
        patientWithPenicillinAllergy.setAllergies("Penicillin");

        Doctor doctor = new Doctor();
        doctor.setId(1L);
        doctor.setFirstName("Sarah");
        doctor.setLastName("Chen");
        doctor.setSpecialization("Cardiology");

        activePrescription = new Prescription();
        activePrescription.setId(100L);
        activePrescription.setPrescriptionNumber("RX-20240315-0001");
        activePrescription.setDoctor(doctor);
        activePrescription.setPatient(patient);
        activePrescription.setMedicationName("Lisinopril");
        activePrescription.setMedicationCode("0001");
        activePrescription.setDosage("10mg");
        activePrescription.setFrequency("Once daily");
        activePrescription.setDuration(30);
        activePrescription.setQuantity(30);
        activePrescription.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        lisinoprilStock = new MedicationInventory();
        lisinoprilStock.setId(1L);
        lisinoprilStock.setMedicationCode("0001");
        lisinoprilStock.setMedicationName("Lisinopril 10mg");
        lisinoprilStock.setCurrentStock(500);
        lisinoprilStock.setReorderLevel(50);
        lisinoprilStock.setUnitPrice(new BigDecimal("0.15"));
        lisinoprilStock.setLocationShelf("A1");
        lisinoprilStock.setRequiresPrescription(true);
    }

    // ════════════════════════════════════════════════════════════════
    // VERIFY PRESCRIPTION TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testVerifyPrescription_NoIssues_ReturnsPassed")
    void testVerifyPrescription_Clean_PassesAllChecks() {
        when(prescriptionRepo.findById(100L)).thenReturn(Optional.of(activePrescription));
        when(prescriptionRepo.findByPatientAndStatus(patient, Prescription.PrescriptionStatus.ACTIVE))
            .thenReturn(List.of(activePrescription));
        when(drugInteractionService.checkInteractions(eq("Lisinopril"), any()))
            .thenReturn(List.of());
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));

        Map<String, Object> report = pharmacyService.verifyPrescription(100L);

        assertThat(report.get("verificationStatus")).isEqualTo("PASSED");
        assertThat(report.get("safeToDispense")).isEqualTo(true);
        assertThat(report.get("allergyConflict")).isEqualTo(false);
        assertThat(report.get("hasCriticalInteraction")).isEqualTo(false);
        assertThat(report.get("inventoryAvailable")).isEqualTo(true);
    }

    @Test
    @DisplayName("testVerifyPrescription_WithAllergies_FlagsWarning")
    void testVerifyPrescription_WithAllergies_ThrowsWarning() {
        // Patient is allergic to Penicillin, prescription is for Amoxicillin (penicillin class)
        Prescription amoxicillinRx = new Prescription();
        amoxicillinRx.setId(101L);
        amoxicillinRx.setPrescriptionNumber("RX-20240315-0002");
        amoxicillinRx.setPatient(patientWithPenicillinAllergy);
        Doctor d = new Doctor(); d.setId(1L); d.setFirstName("Sarah"); d.setLastName("Chen"); d.setSpecialization("GP");
        amoxicillinRx.setDoctor(d);
        amoxicillinRx.setMedicationName("Penicillin V");  // direct allergy match
        amoxicillinRx.setMedicationCode("0003");
        amoxicillinRx.setQuantity(21);
        amoxicillinRx.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        when(prescriptionRepo.findById(101L)).thenReturn(Optional.of(amoxicillinRx));
        when(prescriptionRepo.findByPatientAndStatus(eq(patientWithPenicillinAllergy), eq(Prescription.PrescriptionStatus.ACTIVE)))
            .thenReturn(List.of(amoxicillinRx));
        when(drugInteractionService.checkInteractions(any(), any())).thenReturn(List.of());

        MedicationInventory stock = new MedicationInventory();
        stock.setMedicationCode("0003");
        stock.setCurrentStock(100);
        stock.setReorderLevel(50);
        when(inventoryRepo.findByMedicationCode("0003")).thenReturn(Optional.of(stock));

        Map<String, Object> report = pharmacyService.verifyPrescription(101L);

        assertThat(report.get("allergyConflict")).isEqualTo(true);
        assertThat(report.get("safeToDispense")).isEqualTo(false);
        assertThat(report.get("verificationStatus")).isEqualTo("WARNINGS_PRESENT");

        @SuppressWarnings("unchecked")
        List<String> warnings = (List<String>) report.get("allergyWarnings");
        assertThat(warnings).isNotEmpty();
        assertThat(warnings.get(0)).contains("ALLERGY ALERT");
    }

    @Test
    @DisplayName("testVerifyPrescription_WithDrugInteraction_FlagsWarning")
    void testVerifyPrescription_WithDrugInteraction_ThrowsWarning() {
        // Patient already on Warfarin; new prescription is Aspirin → MAJOR interaction
        DrugInteractionService.Interaction warfarinAspirin =
            new DrugInteractionService.Interaction(
                "warfarin", "aspirin", "MAJOR",
                "Warfarin + Aspirin",
                "Increased bleeding risk."
            );

        when(prescriptionRepo.findById(100L)).thenReturn(Optional.of(activePrescription));
        when(prescriptionRepo.findByPatientAndStatus(patient, Prescription.PrescriptionStatus.ACTIVE))
            .thenReturn(List.of(activePrescription));
        when(drugInteractionService.checkInteractions(eq("Lisinopril"), any()))
            .thenReturn(List.of(warfarinAspirin));
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));

        Map<String, Object> report = pharmacyService.verifyPrescription(100L);

        assertThat(report.get("hasCriticalInteraction")).isEqualTo(true);
        assertThat(report.get("safeToDispense")).isEqualTo(false);
        assertThat(report.get("verificationStatus")).isEqualTo("WARNINGS_PRESENT");
    }

    // ════════════════════════════════════════════════════════════════
    // DISPENSE TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testDispensePrescription_UpdatesInventory – stock decremented correctly")
    void testDispensePrescription_UpdatesInventory() {
        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepo.findById(100L)).thenReturn(Optional.of(activePrescription));
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));
        when(inventoryRepo.save(any())).thenReturn(lisinoprilStock);
        when(fulfillmentRepo.save(any())).thenAnswer(inv -> { PrescriptionFulfillment f = inv.getArgument(0); f.setId(999L); return f; });
        when(prescriptionRepo.save(any())).thenReturn(activePrescription);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        DispenseRequest req = new DispenseRequest();
        req.setQuantityDispensed(30);
        req.setNotes("Counselled patient");

        Map<String, Object> result = pharmacyService.dispensePrescription(100L, req, "john.smith@hospital.com");

        assertThat(result.get("status")).isEqualTo("DISPENSED");
        assertThat(result.get("quantityDispensed")).isEqualTo(30);
        assertThat(result.get("remainingStock")).isEqualTo(470);  // 500 - 30
        assertThat(result.get("fulfillmentId")).isEqualTo(999L);

        assertThat(lisinoprilStock.getCurrentStock()).isEqualTo(470);
        assertThat(activePrescription.getStatus()).isEqualTo(Prescription.PrescriptionStatus.DISPENSED);

        verify(inventoryRepo).save(lisinoprilStock);
        verify(fulfillmentRepo).save(any(PrescriptionFulfillment.class));
        verify(prescriptionRepo).save(activePrescription);
        verify(auditService).log(eq(50L), eq(AuditService.DISPENSE_MEDICATION), anyString(), anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("testDispensePrescription_InsufficientStock_ThrowsException")
    void testDispensePrescription_InsufficientStock_ThrowsException() {
        lisinoprilStock.setCurrentStock(5);  // only 5 units, prescription needs 30

        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepo.findById(100L)).thenReturn(Optional.of(activePrescription));
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));

        DispenseRequest req = new DispenseRequest();
        req.setQuantityDispensed(30);

        assertThatThrownBy(() ->
            pharmacyService.dispensePrescription(100L, req, "john.smith@hospital.com"))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Insufficient stock");

        // Stock must NOT have changed
        assertThat(lisinoprilStock.getCurrentStock()).isEqualTo(5);
        verify(inventoryRepo, never()).save(any());
    }

    @Test
    @DisplayName("testDispensePrescription – cannot dispense already DISPENSED prescription")
    void testDispensePrescription_AlreadyDispensed_ThrowsBadRequest() {
        activePrescription.setStatus(Prescription.PrescriptionStatus.DISPENSED);

        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(prescriptionRepo.findById(100L)).thenReturn(Optional.of(activePrescription));

        DispenseRequest req = new DispenseRequest();
        req.setQuantityDispensed(30);

        assertThatThrownBy(() ->
            pharmacyService.dispensePrescription(100L, req, "john.smith@hospital.com"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("ACTIVE prescriptions");
    }

    // ════════════════════════════════════════════════════════════════
    // INVENTORY / SHIPMENT TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testReceiveShipment_UpdatesStock – stock incremented correctly")
    void testReceiveShipment_UpdatesStock() {
        MedicationInventory amoxicillin = new MedicationInventory();
        amoxicillin.setId(3L);
        amoxicillin.setMedicationCode("0003");
        amoxicillin.setMedicationName("Amoxicillin 500mg");
        amoxicillin.setCurrentStock(0);
        amoxicillin.setReorderLevel(50);

        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(inventoryRepo.findByMedicationCode("0003")).thenReturn(Optional.of(amoxicillin));
        when(inventoryRepo.save(any())).thenReturn(amoxicillin);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        ReceiveShipmentRequest req = new ReceiveShipmentRequest();
        req.setMedicationCode("0003");
        req.setQuantity(200);
        req.setBatchNumber("BATCH-2024-0042");

        Map<String, Object> result = pharmacyService.receiveShipment(req, "john.smith@hospital.com");

        assertThat(result.get("quantityReceived")).isEqualTo(200);
        assertThat(result.get("previousStock")).isEqualTo(0);
        assertThat(result.get("newStock")).isEqualTo(200);
        assertThat(amoxicillin.getCurrentStock()).isEqualTo(200);
        verify(inventoryRepo).save(amoxicillin);
    }

    @Test
    @DisplayName("testAdjustStock_NegativeAdjustment_ReducesStock")
    void testAdjustStock_NegativeAdjustment_ReducesStock() {
        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));
        when(inventoryRepo.save(any())).thenReturn(lisinoprilStock);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        AdjustStockRequest req = new AdjustStockRequest();
        req.setMedicationCode("0001");
        req.setAdjustment(-10);
        req.setReason("10 units damaged in storage");

        Map<String, Object> result = pharmacyService.adjustStock(req, "john.smith@hospital.com");

        assertThat(result.get("previousStock")).isEqualTo(500);
        assertThat(result.get("newStock")).isEqualTo(490);
        assertThat(lisinoprilStock.getCurrentStock()).isEqualTo(490);
    }

    @Test
    @DisplayName("testAdjustStock – negative result below zero throws BadRequestException")
    void testAdjustStock_WouldGoNegative_ThrowsBadRequest() {
        lisinoprilStock.setCurrentStock(5);

        when(pharmacistRepo.findByEmail("john.smith@hospital.com")).thenReturn(Optional.of(pharmacist));
        when(inventoryRepo.findByMedicationCode("0001")).thenReturn(Optional.of(lisinoprilStock));

        AdjustStockRequest req = new AdjustStockRequest();
        req.setMedicationCode("0001");
        req.setAdjustment(-100);  // would result in -95
        req.setReason("Error");

        assertThatThrownBy(() -> pharmacyService.adjustStock(req, "john.smith@hospital.com"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("negative stock");

        assertThat(lisinoprilStock.getCurrentStock()).isEqualTo(5);  // unchanged
    }

    @Test
    @DisplayName("testGetPendingPrescriptions – returns only ACTIVE prescriptions")
    void testGetPendingPrescriptions() {
        when(prescriptionRepo.findByStatus(Prescription.PrescriptionStatus.ACTIVE))
            .thenReturn(List.of(activePrescription));

        List<Map<String, Object>> result = pharmacyService.getPendingPrescriptions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("prescriptionNumber")).isEqualTo("RX-20240315-0001");
    }
}
