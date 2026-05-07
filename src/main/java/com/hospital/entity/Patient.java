package com.hospital.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Patient entity.
 *
 * <p>Patients are referenced by both the Doctor department (appointments,
 * prescriptions) and the Pharmacy department (allergy checks,
 * fulfillments).  Future departments (Finance for billing, Nurses for
 * bed management) will join on the same {@code patients} table.</p>
 *
 * <p>patientId format: PAT-YYYY-XXXX</p>
 */
@Entity
@Table(name = "patients")
@DiscriminatorValue("PATIENT")
@PrimaryKeyJoinColumn(name = "user_id")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Patient extends User {

    @Column(unique = true, nullable = false, length = 20)
    private String patientId;

    @Column
    private LocalDate dateOfBirth;

    /**
     * Blood group – stored as enum for type safety and easy filtering.
     * Finance / HR departments can extend this for donor registries.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private BloodGroup bloodGroup;

    /**
     * Comma-separated list of known allergies.
     * e.g. "Penicillin,Sulfa,NSAIDs"
     *
     * <p>Pharmacy department reads this field during prescription
     * verification.  Future NLP or FHIR integration can parse this
     * into a structured format without breaking the column contract.</p>
     */
    @Column(length = 500)
    private String allergies;

    // ── Allergy helper ───────────────────────────────────────────────

    /**
     * Returns true if the patient is documented as allergic to the
     * given substance (case-insensitive substring match).
     */
    public boolean isAllergicTo(String substance) {
        if (allergies == null || allergies.isBlank()) return false;
        return allergies.toLowerCase().contains(substance.toLowerCase());
    }

    // ── Blood group enum ─────────────────────────────────────────────

    public enum BloodGroup {
        A_POS, A_NEG,
        B_POS, B_NEG,
        O_POS, O_NEG,
        AB_POS, AB_NEG
    }

    // Explicit accessor used by services
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public java.time.LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getAllergies() { return allergies; }
    public void setDateOfBirth(java.time.LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
}
