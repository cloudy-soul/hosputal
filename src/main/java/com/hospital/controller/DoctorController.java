package com.hospital.controller;

import com.hospital.dto.*;
import com.hospital.service.DoctorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Doctor Department REST Controller.
 *
 * <p>All endpoints require the DOCTOR role (enforced via JWT + @PreAuthorize).
 * The logged-in doctor's email is extracted from the JWT principal to
 * ensure doctors can only access their own appointments and prescriptions.</p>
 *
 * <p>Base URL: /api/doctor</p>
 *
 * <pre>
 * GET  /api/doctor/appointments/today          – Today's schedule
 * GET  /api/doctor/appointments/{id}           – Appointment detail
 * POST /api/doctor/appointments/create         – New appointment
 * PUT  /api/doctor/appointments/{id}/complete  – Complete + diagnose
 * GET  /api/doctor/patients/search             – Search patients
 * GET  /api/doctor/patients/{id}/history       – Full patient history
 * POST /api/doctor/prescriptions/create        – Create prescription
 * PUT  /api/doctor/prescriptions/{id}/cancel   – Cancel prescription
 * GET  /api/doctor/prescriptions/active        – Active prescriptions for patient
 * GET  /api/doctor/snomed/search               – SNOMED CT code search
 * GET  /api/doctor/loinc/search                – LOINC lab code search
 * </pre>
 */
@RestController
@RequestMapping("/api/doctor")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }
    // ── Appointments ─────────────────────────────────────────────────

    /** GET /api/doctor/appointments/today
     *  Returns all of today's appointments for the authenticated doctor. */
    @GetMapping("/appointments/today")
    public ResponseEntity<List<Map<String, Object>>> getTodayAppointments(Authentication auth) {
        return ResponseEntity.ok(doctorService.getTodayAppointments(auth.getName()));
    }

    /** GET /api/doctor/appointments/{id}
     *  Returns detailed information for a single appointment. */
    @GetMapping("/appointments/{id}")
    public ResponseEntity<Map<String, Object>> getAppointment(@PathVariable Long id,
                                                               Authentication auth) {
        return ResponseEntity.ok(doctorService.getAppointmentById(id, auth.getName()));
    }

    /** POST /api/doctor/appointments/create
     *  Books a new appointment for a patient.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "patientId": "PAT-2024-0001",
     *    "appointmentDate": "2025-02-01",
     *    "appointmentTime": "09:30",
     *    "reasonForVisit": "Routine diabetes check-up"
     *  }
     *  </pre>
     */
    @PostMapping("/appointments/create")
    public ResponseEntity<Map<String, Object>> createAppointment(
            @Valid @RequestBody CreateAppointmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(doctorService.createAppointment(request, auth.getName()));
    }

    /** PUT /api/doctor/appointments/{id}/complete
     *  Completes an appointment, recording a SNOMED CT diagnosis and
     *  generating an ICD-11 billing code for the Finance Department.
     *  Optionally creates a prescription in the same transaction.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "diagnosis": "Patient presents with uncontrolled type 2 diabetes",
     *    "diagnosisSnomedCode": "44054006",
     *    "diagnosisSnomedDisplay": "Diabetes mellitus type 2",
     *    "notes": "HbA1c elevated – increase Metformin dose",
     *    "prescription": {
     *      "patientId": "PAT-2024-0001",
     *      "medicationName": "Metformin",
     *      "medicationCode": "0002",
     *      "dosage": "500mg",
     *      "frequency": "Twice daily",
     *      "duration": 30,
     *      "quantity": 60,
     *      "instructions": "Take with meals"
     *    }
     *  }
     *  </pre>
     */
    @PutMapping("/appointments/{id}/complete")
    public ResponseEntity<Map<String, Object>> completeAppointment(
            @PathVariable Long id,
            @Valid @RequestBody CompleteAppointmentRequest request,
            Authentication auth) {
        return ResponseEntity.ok(doctorService.completeAppointment(id, request, auth.getName()));
    }

    // ── Patients ─────────────────────────────────────────────────────

    /** GET /api/doctor/patients/search?term=John
     *  Searches patients by name or patient ID. */
    @GetMapping("/patients/search")
    public ResponseEntity<List<Map<String, Object>>> searchPatients(@RequestParam String term) {
        return ResponseEntity.ok(doctorService.searchPatients(term));
    }

    /** GET /api/doctor/patients/{patientId}/history
     *  Returns the full medical history for a patient:
     *  all appointments and all prescriptions. */
    @GetMapping("/patients/{patientId}/history")
    public ResponseEntity<Map<String, Object>> getPatientHistory(@PathVariable String patientId) {
        return ResponseEntity.ok(doctorService.getPatientHistory(patientId));
    }

    // ── Prescriptions ────────────────────────────────────────────────

    /** POST /api/doctor/prescriptions/create
     *  Creates a new prescription.  Once saved with status ACTIVE,
     *  it immediately appears in the Pharmacy Department's queue.
     *
     *  <p>Request body example:</p>
     *  <pre>
     *  {
     *    "patientId": "PAT-2024-0001",
     *    "medicationName": "Lisinopril",
     *    "medicationCode": "0001",
     *    "dosage": "10mg",
     *    "frequency": "Once daily",
     *    "duration": 30,
     *    "quantity": 30,
     *    "instructions": "Take in the morning"
     *  }
     *  </pre>
     */
    @PostMapping("/prescriptions/create")
    public ResponseEntity<Map<String, Object>> createPrescription(
            @Valid @RequestBody CreatePrescriptionRequest request,
            Authentication auth) {
        return ResponseEntity.ok(doctorService.createPrescription(request, auth.getName()));
    }

    /** PUT /api/doctor/prescriptions/{id}/cancel
     *  Cancels an ACTIVE prescription.  Cannot cancel already dispensed prescriptions. */
    @PutMapping("/prescriptions/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelPrescription(@PathVariable Long id,
                                                                   Authentication auth) {
        return ResponseEntity.ok(doctorService.cancelPrescription(id, auth.getName()));
    }

    /** GET /api/doctor/prescriptions/active?patientId=PAT-2024-0001
     *  Returns all active prescriptions for a specific patient. */
    @GetMapping("/prescriptions/active")
    public ResponseEntity<List<Map<String, Object>>> getActivePrescriptions(
            @RequestParam String patientId) {
        return ResponseEntity.ok(doctorService.getActivePrescriptions(patientId));
    }

    // ── Medical Terminology ──────────────────────────────────────────

    /** GET /api/doctor/snomed/search?term=diabetes
     *  Searches SNOMED CT codes by clinical term.
     *  Returns { code → display name } pairs. */
    @GetMapping("/snomed/search")
    public ResponseEntity<Map<String, String>> searchSnomed(@RequestParam String term) {
        return ResponseEntity.ok(doctorService.searchSnomedCodes(term));
    }

    /** GET /api/doctor/loinc/search?term=hemoglobin
     *  Searches LOINC lab codes by term.
     *  Returns { code → display name } pairs. */
    @GetMapping("/loinc/search")
    public ResponseEntity<Map<String, String>> searchLoinc(@RequestParam String term) {
        return ResponseEntity.ok(doctorService.searchLoincCodes(term));
    }
}
