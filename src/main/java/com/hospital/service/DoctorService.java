package com.hospital.service;

import com.hospital.dto.*;
import com.hospital.entity.*;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.*;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Doctor Department – Business Logic Layer.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Manage appointments: today's schedule, completion, diagnosis coding</li>
 *   <li>Create prescriptions with SNOMED CT diagnosis linkage</li>
 *   <li>Patient search and full medical history</li>
 *   <li>SNOMED CT and LOINC terminology lookups</li>
 *   <li>ICD-11 code assignment at appointment completion (for Finance dept)</li>
 * </ul>
 *
 * <p><b>Integration contract for other departments:</b></p>
 * <ul>
 *   <li>Finance:  reads Appointment.diagnosisIcd11Code and Prescription data</li>
 *   <li>Pharmacy: reads Prescription table (status = ACTIVE)</li>
 *   <li>Nurses:   reads Appointment status for bed management</li>
 * </ul>
 */
@Service
@Transactional
public class DoctorService {

    private final AppointmentRepository     appointmentRepo;
    private final PrescriptionRepository    prescriptionRepo;
    private final PatientRepository         patientRepo;
    private final DoctorRepository          doctorRepo;
    private final SnomedCodeService         snomedService;
    private final LoincCodeService          loincService;
    private final Icd11MappingService       icd11Service;
    private final AuditService              auditService;
    private final IdGenerator               idGenerator;

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    public DoctorService(AppointmentRepository appointmentRepo,
                         PrescriptionRepository prescriptionRepo,
                         PatientRepository patientRepo,
                         DoctorRepository doctorRepo,
                         SnomedCodeService snomedService,
                         LoincCodeService loincService,
                         Icd11MappingService icd11Service,
                         AuditService auditService,
                         IdGenerator idGenerator) {
        this.appointmentRepo = appointmentRepo;
        this.prescriptionRepo = prescriptionRepo;
        this.patientRepo = patientRepo;
        this.doctorRepo = doctorRepo;
        this.snomedService = snomedService;
        this.loincService = loincService;
        this.icd11Service = icd11Service;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    // ════════════════════════════════════════════════════════════════
    // APPOINTMENT MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    /**
     * Returns all appointments scheduled for the logged-in doctor today.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTodayAppointments(String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        List<Appointment> appointments =
            appointmentRepo.findByDoctorAndAppointmentDate(doctor, LocalDate.now());

    return appointments.stream().map(this::toAppointmentMap).collect(Collectors.toList());
    }

    /**
     * Returns full appointment details including patient information.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAppointmentById(Long id, String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        Appointment appt = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        // Doctors may only access their own appointments
        if (!appt.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("You are not the assigned doctor for this appointment.");
        }
        return toAppointmentMap(appt);
    }

    /**
     * Creates a new appointment for a patient.
     */
    public Map<String, Object> createAppointment(CreateAppointmentRequest req, String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        Patient patient = patientRepo.findByPatientId(req.getPatientId())
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + req.getPatientId()));

        Appointment appt = new Appointment();
        appt.setAppointmentNumber(idGenerator.generateAppointmentNumber());
        appt.setDoctor(doctor);
        appt.setPatient(patient);
        appt.setAppointmentDate(req.getAppointmentDate());
        appt.setAppointmentTime(req.getAppointmentTime());
        appt.setReasonForVisit(req.getReasonForVisit());
        appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepo.save(appt);
        auditService.log(doctor.getId(), AuditService.CREATE_APPOINTMENT,
            "Appointment", saved.getId(),
            "SYSTEM", "Appointment " + saved.getAppointmentNumber() + " created");

        log.info("Appointment {} created for patient {} by Dr {}",
            saved.getAppointmentNumber(), patient.getPatientId(), doctor.getDoctorId());

        return toAppointmentMap(saved);
    }

    /**
     * Completes an appointment:
     * <ol>
     *   <li>Sets diagnosis with SNOMED CT code</li>
     *   <li>Maps to ICD-11 code for Finance Department billing</li>
     *   <li>Optionally creates a prescription in one transaction</li>
     *   <li>Marks appointment COMPLETED</li>
     * </ol>
     */
    public Map<String, Object> completeAppointment(Long id, CompleteAppointmentRequest req,
                                                   String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        Appointment appt = appointmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));

        if (!appt.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("You are not the assigned doctor for this appointment.");
        }
        if (appt.getStatus() != Appointment.AppointmentStatus.SCHEDULED) {
            throw new BadRequestException("Appointment is already " + appt.getStatus());
        }
        if (req.getDiagnosis() == null || req.getDiagnosis().isBlank()) {
            throw new BadRequestException("Diagnosis is required to complete an appointment.");
        }

        // ── SNOMED CT ──────────────────────────────────────────────
        appt.setDiagnosis(req.getDiagnosis());
        appt.setDiagnosisSnomedCode(req.getDiagnosisSnomedCode());
        appt.setDiagnosisSnomedDisplay(req.getDiagnosisSnomedDisplay());

        // ── ICD-11 mapping (for Finance Department billing) ─────────
        icd11Service.mapFromSnomed(req.getDiagnosisSnomedCode()).ifPresent(mapping -> {
            appt.setDiagnosisIcd11Code(mapping[0]);
            appt.setDiagnosisIcd11Display(mapping[1]);
        });

        appt.setNotes(req.getNotes());
        appt.setStatus(Appointment.AppointmentStatus.COMPLETED);

        appointmentRepo.save(appt);

        auditService.log(doctor.getId(), AuditService.COMPLETE_APPOINTMENT,
            "Appointment", appt.getId(), "SYSTEM",
            "SNOMED:" + req.getDiagnosisSnomedCode() + " ICD-11:" + appt.getDiagnosisIcd11Code());

        // ── Optional inline prescription ────────────────────────────
        Map<String, Object> result = toAppointmentMap(appt);
        if (req.getPrescription() != null) {
            req.getPrescription().setAppointmentId(appt.getId());
            Map<String, Object> rx = createPrescription(req.getPrescription(), doctorEmail);
            result.put("prescription", rx);
        }

        return result;
    }

    // ════════════════════════════════════════════════════════════════
    // PRESCRIPTION MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    /**
     * Creates a new prescription.
     *
     * <p>The prescription is written to the database with status ACTIVE.
     * The Pharmacy Department's pending-prescriptions queue will
     * automatically include it via {@code PrescriptionRepository.findByStatus(ACTIVE)}.</p>
     */
    public Map<String, Object> createPrescription(CreatePrescriptionRequest req, String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        Patient patient = patientRepo.findByPatientId(req.getPatientId())
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + req.getPatientId()));

        // ── Allergy pre-check ──────────────────────────────────────
        // Doctor is warned if prescribing a drug the patient is allergic to.
        // The pharmacist will verify again before dispensing.
        if (patient.isAllergicTo(req.getMedicationName())) {
            log.warn("ALLERGY ALERT: Patient {} is allergic to {}",
                patient.getPatientId(), req.getMedicationName());
            // We warn but do NOT block – the doctor has clinical authority.
            // The pharmacy will enforce the block.
        }

        Prescription rx = new Prescription();
        rx.setPrescriptionNumber(idGenerator.generatePrescriptionNumber());
        rx.setDoctor(doctor);
        rx.setPatient(patient);
        rx.setMedicationName(req.getMedicationName());
        rx.setMedicationCode(req.getMedicationCode());
        rx.setDosage(req.getDosage());
        rx.setFrequency(req.getFrequency());
        rx.setDuration(req.getDuration());
        rx.setQuantity(req.getQuantity());
        rx.setRefillsRemaining(req.getRefillsRemaining());
        rx.setInstructions(req.getInstructions());
        rx.setControlledSubstance(req.isControlledSubstance());
        rx.setStatus(Prescription.PrescriptionStatus.ACTIVE);

        // Link to appointment if provided
        if (req.getAppointmentId() != null) {
            appointmentRepo.findById(req.getAppointmentId()).ifPresent(rx::setAppointment);
        }

        Prescription saved = prescriptionRepo.save(rx);

        auditService.log(doctor.getId(), AuditService.CREATE_PRESCRIPTION,
            "Prescription", saved.getId(), "SYSTEM",
            saved.getPrescriptionNumber() + " for patient " + patient.getPatientId());

        log.info("Prescription {} created – now visible in pharmacy queue", saved.getPrescriptionNumber());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prescriptionNumber", saved.getPrescriptionNumber());
        response.put("status", saved.getStatus());
        response.put("message", "Prescription created and sent to pharmacy");
        response.put("patientName", patient.getFullName());
        response.put("medication", saved.getMedicationName() + " " + saved.getDosage());
        response.put("isControlledSubstance", saved.isControlledSubstance());
        if (patient.isAllergicTo(req.getMedicationName())) {
            response.put("allergyWarning", "⚠ Patient has documented allergy to " + req.getMedicationName());
        }
        return response;
    }

    /**
     * Cancels an ACTIVE prescription. Cannot cancel DISPENSED prescriptions.
     */
    public Map<String, Object> cancelPrescription(Long id, String doctorEmail) {
        Doctor doctor = getDoctorByEmail(doctorEmail);
        Prescription rx = prescriptionRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));

        if (!rx.getDoctor().getId().equals(doctor.getId())) {
            throw new BadRequestException("You did not create this prescription.");
        }
        if (rx.getStatus() == Prescription.PrescriptionStatus.DISPENSED) {
            throw new BadRequestException("Cannot cancel a prescription that has already been dispensed.");
        }
        if (rx.getStatus() == Prescription.PrescriptionStatus.CANCELLED) {
            throw new BadRequestException("Prescription is already cancelled.");
        }

        rx.setStatus(Prescription.PrescriptionStatus.CANCELLED);
        prescriptionRepo.save(rx);

        auditService.log(doctor.getId(), AuditService.CANCEL_PRESCRIPTION,
            "Prescription", rx.getId(), "SYSTEM", rx.getPrescriptionNumber());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("prescriptionNumber", rx.getPrescriptionNumber());
        response.put("status", "CANCELLED");
        response.put("message", "Prescription cancelled successfully.");
        return response;
    }

    /**
     * Returns all ACTIVE prescriptions for a specific patient.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivePrescriptions(String patientId) {
        Patient patient = patientRepo.findByPatientId(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        return prescriptionRepo
            .findByPatientAndStatus(patient, Prescription.PrescriptionStatus.ACTIVE)
            .stream().map(this::toPrescriptionMap).collect(Collectors.toList());
    }

    // ════════════════════════════════════════════════════════════════
    // PATIENT MANAGEMENT
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchPatients(String term) {
        return patientRepo.searchByNameOrId(term)
            .stream().map(this::toPatientMap).collect(Collectors.toList());
    }

    /**
     * Returns a patient's complete medical history:
     * all appointments and all prescriptions (all statuses), newest first.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPatientHistory(String patientId) {
        Patient patient = patientRepo.findByPatientId(patientId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        List<Map<String, Object>> appointments =
            appointmentRepo.findByPatient(patient).stream().map(this::toAppointmentMap).collect(Collectors.toList());
        List<Map<String, Object>> prescriptions =
            prescriptionRepo.findAllByPatientOrderByDateDesc(patient).stream().map(this::toPrescriptionMap).collect(Collectors.toList());

        Map<String, Object> history = new LinkedHashMap<>();
        history.put("patient", toPatientMap(patient));
        history.put("totalAppointments", appointments.size());
        history.put("totalPrescriptions", prescriptions.size());
        history.put("appointments", appointments);
        history.put("prescriptions", prescriptions);
        return history;
    }

    // ════════════════════════════════════════════════════════════════
    // TERMINOLOGY SERVICES
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, String> searchSnomedCodes(String term) {
        return snomedService.searchByTerm(term);
    }

    @Transactional(readOnly = true)
    public Map<String, String> searchLoincCodes(String term) {
        return loincService.searchByTerm(term);
    }

    // ════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════

    private Doctor getDoctorByEmail(String email) {
        return doctorRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + email));
    }

    private Map<String, Object> toAppointmentMap(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("appointmentNumber", a.getAppointmentNumber());
        m.put("patientId", a.getPatient().getPatientId());
        m.put("patientName", a.getPatient().getFullName());
        m.put("doctorName", "Dr. " + a.getDoctor().getFullName());
        m.put("appointmentDate", a.getAppointmentDate());
        m.put("appointmentTime", a.getAppointmentTime());
        m.put("status", a.getStatus());
        m.put("reasonForVisit", a.getReasonForVisit());
        m.put("diagnosis", a.getDiagnosis());
        m.put("diagnosisSnomedCode", a.getDiagnosisSnomedCode());
        m.put("diagnosisSnomedDisplay", a.getDiagnosisSnomedDisplay());
        m.put("diagnosisIcd11Code", a.getDiagnosisIcd11Code());
        m.put("diagnosisIcd11Display", a.getDiagnosisIcd11Display());
        m.put("notes", a.getNotes());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }

    private Map<String, Object> toPrescriptionMap(Prescription p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("prescriptionNumber", p.getPrescriptionNumber());
        m.put("patientId", p.getPatient().getPatientId());
        m.put("patientName", p.getPatient().getFullName());
        m.put("prescribedBy", "Dr. " + p.getDoctor().getFullName());
        m.put("medicationName", p.getMedicationName());
        m.put("medicationCode", p.getMedicationCode());
        m.put("dosage", p.getDosage());
        m.put("frequency", p.getFrequency());
        m.put("duration", p.getDuration() + " days");
        m.put("quantity", p.getQuantity());
        m.put("refillsRemaining", p.getRefillsRemaining());
        m.put("instructions", p.getInstructions());
        m.put("status", p.getStatus());
        m.put("prescribedDate", p.getPrescribedDate());
        m.put("isControlledSubstance", p.isControlledSubstance());
        return m;
    }

    private Map<String, Object> toPatientMap(Patient p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", p.getPatientId());
        m.put("fullName", p.getFullName());
        m.put("email", p.getEmail());
        m.put("dateOfBirth", p.getDateOfBirth());
        m.put("bloodGroup", p.getBloodGroup());
        m.put("allergies", p.getAllergies());
        return m;
    }
}
