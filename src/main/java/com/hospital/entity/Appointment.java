package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Appointment entity – bridges the Doctor and Patient records.
 *
 * <p>Medical coding fields stored here:</p>
 * <ul>
 *   <li><b>SNOMED CT</b> – clinical diagnosis code (e.g. "44054006" → Diabetes type 2)</li>
 *   <li><b>ICD-11</b>    – billing classification code mapped from SNOMED at completion</li>
 * </ul>
 *
 * <p>Future departments that will reference this entity:</p>
 * <ul>
 *   <li>Finance – uses diagnosisIcd11Code for invoice generation</li>
 *   <li>Nurses  – uses appointment status to track bed occupancy</li>
 * </ul>
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format: APT-YYYYMMDD-XXXX */
    @Column(unique = true, nullable = false, length = 25)
    private String appointmentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(length = 500)
    private String reasonForVisit;

    /** Free-text diagnosis written by the doctor. */
    @Column(length = 1000)
    private String diagnosis;

    /**
     * SNOMED CT code for the diagnosis.
     * Reference: {@link com.hospital.service.SnomedCodeService}
     * e.g. "44054006"
     */
    @Column(length = 20)
    private String diagnosisSnomedCode;

    /**
     * Human-readable SNOMED display name stored alongside the code
     * so it remains readable even without a terminology server lookup.
     * e.g. "Diabetes mellitus type 2"
     */
    @Column(length = 200)
    private String diagnosisSnomedDisplay;

    /**
     * ICD-11 billing code mapped from SNOMED at appointment completion.
     * Used by the Finance Department for invoicing.
     * e.g. "5A11" (Type 2 diabetes mellitus)
     */
    @Column(length = 20)
    private String diagnosisIcd11Code;

    /** ICD-11 display name for readability in finance reports. */
    @Column(length = 200)
    private String diagnosisIcd11Display;

    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // No-arg constructor for JPA
    public Appointment() {}

    // Explicit getters and setters (avoid relying on Lombok during annotation-processing failures)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAppointmentNumber() { return appointmentNumber; }
    public void setAppointmentNumber(String appointmentNumber) { this.appointmentNumber = appointmentNumber; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }

    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }

    public LocalTime getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(LocalTime appointmentTime) { this.appointmentTime = appointmentTime; }

    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }

    public String getReasonForVisit() { return reasonForVisit; }
    public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getDiagnosisSnomedCode() { return diagnosisSnomedCode; }
    public void setDiagnosisSnomedCode(String diagnosisSnomedCode) { this.diagnosisSnomedCode = diagnosisSnomedCode; }

    public String getDiagnosisSnomedDisplay() { return diagnosisSnomedDisplay; }
    public void setDiagnosisSnomedDisplay(String diagnosisSnomedDisplay) { this.diagnosisSnomedDisplay = diagnosisSnomedDisplay; }

    public String getDiagnosisIcd11Code() { return diagnosisIcd11Code; }
    public void setDiagnosisIcd11Code(String diagnosisIcd11Code) { this.diagnosisIcd11Code = diagnosisIcd11Code; }

    public String getDiagnosisIcd11Display() { return diagnosisIcd11Display; }
    public void setDiagnosisIcd11Display(String diagnosisIcd11Display) { this.diagnosisIcd11Display = diagnosisIcd11Display; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }

    // ── Status enum ──────────────────────────────────────────────────

    public enum AppointmentStatus {
        SCHEDULED,
        COMPLETED,
        CANCELLED
    }
}
