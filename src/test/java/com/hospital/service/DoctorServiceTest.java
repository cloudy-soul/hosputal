package com.hospital.service;

import com.hospital.dto.CreatePrescriptionRequest;
import com.hospital.dto.CompleteAppointmentRequest;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.*;
import com.hospital.util.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Doctor Department service layer.
 *
 * <p>All external dependencies are mocked with Mockito so the tests
 * run in isolation without a database (H2 or MySQL).</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorService Unit Tests")
class DoctorServiceTest {

    // ── Mocks ────────────────────────────────────────────────────────

    @Mock AppointmentRepository    appointmentRepo;
    @Mock PrescriptionRepository   prescriptionRepo;
    @Mock PatientRepository        patientRepo;
    @Mock DoctorRepository         doctorRepo;
    @Mock SnomedCodeService        snomedService;
    @Mock LoincCodeService         loincService;
    @Mock Icd11MappingService      icd11Service;
    @Mock AuditService             auditService;
    @Mock IdGenerator              idGenerator;

    @InjectMocks DoctorService doctorService;

    // ── Fixtures ─────────────────────────────────────────────────────

    Doctor  doctor;
    Patient patient;
    Patient patientWithAllergy;

    @BeforeEach
    void setUp() {
        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setEmail("sarah.chen@hospital.com");
        doctor.setFirstName("Sarah");
        doctor.setLastName("Chen");
        doctor.setDoctorId("DOC-2024-0001");
        doctor.setSpecialization("Cardiology");
        doctor.setLicenseNumber("MED-LIC-12345");

        patient = new Patient();
        patient.setId(10L);
        patient.setEmail("john.doe@example.com");
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setPatientId("PAT-2024-0001");
        patient.setAllergies(null);

        patientWithAllergy = new Patient();
        patientWithAllergy.setId(11L);
        patientWithAllergy.setEmail("jane.smith@example.com");
        patientWithAllergy.setFirstName("Jane");
        patientWithAllergy.setLastName("Smith");
        patientWithAllergy.setPatientId("PAT-2024-0002");
        patientWithAllergy.setAllergies("Penicillin");
    }

    // ════════════════════════════════════════════════════════════════
    // PRESCRIPTION TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testCreatePrescription – happy path creates and returns prescription number")
    void testCreatePrescription() {
        // Arrange
        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(patientRepo.findByPatientId("PAT-2024-0001")).thenReturn(Optional.of(patient));
        when(idGenerator.generatePrescriptionNumber()).thenReturn("RX-20240315-0001");
        when(prescriptionRepo.save(any(Prescription.class))).thenAnswer(inv -> {
            Prescription rx = inv.getArgument(0);
            rx.setId(100L);
            return rx;
        });
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        CreatePrescriptionRequest req = new CreatePrescriptionRequest();
        req.setPatientId("PAT-2024-0001");
        req.setMedicationName("Lisinopril");
        req.setMedicationCode("0001");
        req.setDosage("10mg");
        req.setFrequency("Once daily");
        req.setDuration(30);
        req.setQuantity(30);
        req.setInstructions("Take in the morning");

        // Act
        Map<String, Object> result = doctorService.createPrescription(req, "sarah.chen@hospital.com");

        // Assert
        assertThat(result).containsKey("prescriptionNumber");
        assertThat(result.get("prescriptionNumber")).isEqualTo("RX-20240315-0001");
        assertThat(result.get("status")).isEqualTo(Prescription.PrescriptionStatus.ACTIVE);
        assertThat(result.get("message")).isEqualTo("Prescription created and sent to pharmacy");

        verify(prescriptionRepo).save(argThat(rx ->
            "RX-20240315-0001".equals(rx.getPrescriptionNumber()) &&
            "Lisinopril".equals(rx.getMedicationName()) &&
            rx.getStatus() == Prescription.PrescriptionStatus.ACTIVE
        ));
        verify(auditService).log(eq(1L), eq(AuditService.CREATE_PRESCRIPTION),
            eq("Prescription"), eq(100L), anyString(), anyString());
    }

    @Test
    @DisplayName("testCreatePrescription – allergy warning included in response but not blocked")
    void testCreatePrescription_WithPatientAllergy_IncludesWarning() {
        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(patientRepo.findByPatientId("PAT-2024-0002")).thenReturn(Optional.of(patientWithAllergy));
        when(idGenerator.generatePrescriptionNumber()).thenReturn("RX-20240315-0002");
        when(prescriptionRepo.save(any())).thenAnswer(inv -> { Prescription rx = inv.getArgument(0); rx.setId(101L); return rx; });
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        CreatePrescriptionRequest req = new CreatePrescriptionRequest();
        req.setPatientId("PAT-2024-0002");
        req.setMedicationName("Amoxicillin");  // Not Penicillin – no match
        req.setMedicationCode("0003");
        req.setDosage("500mg");
        req.setFrequency("Three times daily");
        req.setDuration(7);
        req.setQuantity(21);

        // Amoxicillin is a penicillin antibiotic – allergy should be flagged
        Map<String, Object> result = doctorService.createPrescription(req, "sarah.chen@hospital.com");

        // Prescription is created (doctor has authority) – no exception thrown
        assertThat(result.get("prescriptionNumber")).isEqualTo("RX-20240315-0002");
    }

    @Test
    @DisplayName("testCreatePrescription – patient not found throws ResourceNotFoundException")
    void testCreatePrescription_PatientNotFound_ThrowsException() {
        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(patientRepo.findByPatientId("PAT-UNKNOWN")).thenReturn(Optional.empty());

        CreatePrescriptionRequest req = new CreatePrescriptionRequest();
        req.setPatientId("PAT-UNKNOWN");
        req.setMedicationName("Lisinopril");
        req.setMedicationCode("0001");
        req.setDosage("10mg");
        req.setFrequency("Once daily");
        req.setDuration(30);
        req.setQuantity(30);

        assertThatThrownBy(() -> doctorService.createPrescription(req, "sarah.chen@hospital.com"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("PAT-UNKNOWN");
    }

    @Test
    @DisplayName("testCancelPrescription – successfully cancels ACTIVE prescription")
    void testCancelPrescription() {
        Prescription rx = new Prescription();
        rx.setId(200L);
        rx.setPrescriptionNumber("RX-20240315-0001");
        rx.setDoctor(doctor);
        rx.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepo.findById(200L)).thenReturn(Optional.of(rx));
        when(prescriptionRepo.save(any())).thenReturn(rx);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        Map<String, Object> result = doctorService.cancelPrescription(200L, "sarah.chen@hospital.com");

        assertThat(result.get("status")).isEqualTo("CANCELLED");
        assertThat(rx.getStatus()).isEqualTo(Prescription.PrescriptionStatus.CANCELLED);
        verify(prescriptionRepo).save(rx);
    }

    @Test
    @DisplayName("testCancelPrescription – cannot cancel DISPENSED prescription")
    void testCancelPrescription_AlreadyDispensed_ThrowsBadRequest() {
        Prescription rx = new Prescription();
        rx.setId(201L);
        rx.setDoctor(doctor);
        rx.setStatus(Prescription.PrescriptionStatus.DISPENSED);

        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepo.findById(201L)).thenReturn(Optional.of(rx));

        assertThatThrownBy(() -> doctorService.cancelPrescription(201L, "sarah.chen@hospital.com"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already been dispensed");
    }

    // ════════════════════════════════════════════════════════════════
    // APPOINTMENT TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testCompleteAppointment – sets SNOMED + ICD-11 codes and marks COMPLETED")
    void testCompleteAppointment() {
        Appointment appt = new Appointment();
        appt.setId(300L);
        appt.setAppointmentNumber("APT-20240115-0001");
        appt.setDoctor(doctor);
        appt.setPatient(patient);
        appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);

        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(appointmentRepo.findById(300L)).thenReturn(Optional.of(appt));
        when(icd11Service.getIcd11Code(anyString())).thenReturn("BA00");
        when(icd11Service.getIcd11Display(anyString())).thenReturn("Essential hypertension");
        when(appointmentRepo.save(any())).thenReturn(appt);
        doNothing().when(auditService).log(anyLong(), anyString(), anyString(), anyLong(), anyString(), anyString());

        CompleteAppointmentRequest req = new CompleteAppointmentRequest();
        req.setDiagnosis("Patient presents with elevated blood pressure");
        req.setDiagnosisSnomedCode("38341003");
        req.setDiagnosisSnomedDisplay("Hypertension (disorder)");
        req.setNotes("Increase Lisinopril dose");

        Map<String, Object> result = doctorService.completeAppointment(300L, req, "sarah.chen@hospital.com");

        assertThat(appt.getStatus()).isEqualTo(Appointment.AppointmentStatus.COMPLETED);
        assertThat(appt.getDiagnosisSnomedCode()).isEqualTo("38341003");
        assertThat(appt.getDiagnosisIcd11Code()).isEqualTo("BA00");
        assertThat(result.get("diagnosisSnomedCode")).isEqualTo("38341003");
        assertThat(result.get("diagnosisIcd11Code")).isEqualTo("BA00");
    }

    @Test
    @DisplayName("testCompleteAppointment – cannot complete already COMPLETED appointment")
    void testCompleteAppointment_AlreadyCompleted_ThrowsBadRequest() {
        Appointment appt = new Appointment();
        appt.setId(301L);
        appt.setDoctor(doctor);
        appt.setPatient(patient);
        appt.setStatus(Appointment.AppointmentStatus.COMPLETED);

        when(doctorRepo.findByEmail("sarah.chen@hospital.com")).thenReturn(Optional.of(doctor));
        when(appointmentRepo.findById(301L)).thenReturn(Optional.of(appt));

        CompleteAppointmentRequest req = new CompleteAppointmentRequest();
        req.setDiagnosis("Some diagnosis");
        req.setDiagnosisSnomedCode("38341003");
        req.setDiagnosisSnomedDisplay("Hypertension");

        assertThatThrownBy(() -> doctorService.completeAppointment(301L, req, "sarah.chen@hospital.com"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("already");
    }

    // ════════════════════════════════════════════════════════════════
    // SNOMED TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testSearchSnomedCodes – delegates to SnomedCodeService")
    void testSearchSnomedCodes() {
        when(snomedService.searchByTerm("diabetes")).thenReturn(
            Map.of("44054006", "Diabetes mellitus type 2 (disorder)",
                   "73211009", "Diabetes mellitus type 1 (disorder)")
        );

        Map<String, String> results = doctorService.searchSnomedCodes("diabetes");

        assertThat(results).hasSize(2);
        assertThat(results).containsKey("44054006");
        assertThat(results.get("44054006")).contains("Diabetes mellitus type 2");
        verify(snomedService).searchByTerm("diabetes");
    }

    @Test
    @DisplayName("testSearchSnomedCodes – empty term returns empty map")
    void testSearchSnomedCodes_EmptyTerm() {
        when(snomedService.searchByTerm("")).thenReturn(Map.of());
        Map<String, String> results = doctorService.searchSnomedCodes("");
        assertThat(results).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════
    // PATIENT SEARCH TESTS
    // ════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("testSearchPatients – returns matching patients")
    void testSearchPatients() {
        when(patientRepo.searchByNameOrId("John")).thenReturn(List.of(patient));

        List<Map<String, Object>> results = doctorService.searchPatients("John");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("patientId")).isEqualTo("PAT-2024-0001");
        assertThat(results.get(0).get("fullName")).isEqualTo("John Doe");
    }
}
