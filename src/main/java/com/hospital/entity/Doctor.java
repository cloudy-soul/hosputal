package com.hospital.entity;

import jakarta.persistence.*;

/**
 * Doctor entity – Department 1.
 *
 * <p>Inherits all user fields from {@link User} (email, password, name,
 * role, timestamps).  The table {@code doctors} stores only the
 * doctor-specific columns joined on {@code users.id}.</p>
 *
 * <p>doctorId format: DOC-YYYY-XXXX  (e.g. DOC-2024-0001)</p>
 */
@Entity
@Table(name = "doctors")
@DiscriminatorValue("DOCTOR")
@PrimaryKeyJoinColumn(name = "user_id")
public class Doctor extends User {

    /**
     * Human-readable unique doctor identifier used on prescriptions,
     * appointments, and patient records.
     * Format: DOC-YYYY-XXXX
     */
    @Column(unique = true, nullable = false, length = 20)
    private String doctorId;

    /** Medical specialization e.g. "Cardiology", "Internal Medicine". */
    @Column(nullable = false, length = 100)
    private String specialization;

    /** Medical license number – required for prescription authority. */
    @Column(nullable = false, length = 50)
    private String licenseNumber;

    @Column
    private int yearsOfExperience;

    // No-arg constructor for JPA
    public Doctor() {}

    // Explicit getters/setters to avoid reliance on Lombok at compile-time
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
}
