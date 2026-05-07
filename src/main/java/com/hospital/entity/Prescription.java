package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Prescription entity – the primary integration point between
 * the Doctor Department and the Pharmacy Department.
 *
 * <p><b>Medical coding fields:</b></p>
 * <ul>
 *   <li>{@code medicationCode} – RxNorm or NDC code for the drug</li>
 *   <li>Diagnosis codes are stored on the linked {@link Appointment}</li>
 * </ul>
 *
 * <p><b>Lifecycle:</b> ACTIVE → DISPENSED (by pharmacy) or CANCELLED / EXPIRED</p>
 *
 * <p><b>Integration hooks for future departments:</b></p>
 * <ul>
 *   <li>Finance  – reads prescriptions to include medication costs in patient bills</li>
 *   <li>Storage  – uses {@code medicationCode} for warehouse tracking</li>
 *   <li>Purchase – monitors stock depletion to trigger reorders</li>
 * </ul>
 */
@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Format: RX-YYYYMMDD-XXXX */
    @Column(unique = true, nullable = false, length = 25)
    private String prescriptionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    /**
     * Optional link to the appointment that generated this prescription.
     * Null for stand-alone prescriptions written outside an appointment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(nullable = false, length = 200)
    private String medicationName;

    /**
     * RxNorm or NDC code for the drug.
     * Pharmacy uses this to locate the medication in {@link MedicationInventory}.
     */
    @Column(nullable = false, length = 30)
    private String medicationCode;

    /** Dosage strength e.g. "10mg", "500mg". */
    @Column(nullable = false, length = 50)
    private String dosage;

    /** Frequency e.g. "Once daily", "Twice daily", "Every 8 hours". */
    @Column(nullable = false, length = 100)
    private String frequency;

    /** Duration of treatment in days. */
    @Column(nullable = false)
    private int duration;

    /** Total quantity to dispense (pills / units). */
    @Column(nullable = false)
    private int quantity;

    /** Number of refills still allowed. */
    @Column
    private int refillsRemaining;

    /** Patient instructions e.g. "Take with food", "Avoid alcohol". */
    @Column(length = 500)
    private String instructions;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime prescribedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrescriptionStatus status = PrescriptionStatus.ACTIVE;

    /**
     * Flags Schedule II–V drugs.
     * Pharmacy department applies additional verification for controlled substances.
     */
    @Column(nullable = false)
    private boolean isControlledSubstance = false;

    // ── Status enum ──────────────────────────────────────────────────

    public enum PrescriptionStatus {
        /** Created by doctor, waiting for pharmacy action. */
        ACTIVE,
        /** Dispensed by pharmacist. */
        DISPENSED,
        /** Past the validity period. */
        EXPIRED,
        /** Cancelled by doctor before dispensing. */
        CANCELLED
    }

    // Explicit accessors (some tools may not run Lombok during analysis)
    public Long getId() { return id; }
    public String getPrescriptionNumber() { return prescriptionNumber; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public Appointment getAppointment() { return appointment; }
    public String getMedicationName() { return medicationName; }
    public String getMedicationCode() { return medicationCode; }
    public String getDosage() { return dosage; }
    public String getFrequency() { return frequency; }
    public int getDuration() { return duration; }
    public int getQuantity() { return quantity; }
    public int getRefillsRemaining() { return refillsRemaining; }
    public String getInstructions() { return instructions; }
    public java.time.LocalDateTime getPrescribedDate() { return prescribedDate; }
    public PrescriptionStatus getStatus() { return status; }
    public boolean isControlledSubstance() { return isControlledSubstance; }
    public void setStatus(PrescriptionStatus status) { this.status = status; }
    public void setId(Long id) { this.id = id; }
    // Explicit setters used by services
    public void setPrescriptionNumber(String prescriptionNumber) { this.prescriptionNumber = prescriptionNumber; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public void setMedicationCode(String medicationCode) { this.medicationCode = medicationCode; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setRefillsRemaining(int refillsRemaining) { this.refillsRemaining = refillsRemaining; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public void setControlledSubstance(boolean controlled) { this.isControlledSubstance = controlled; }
}
