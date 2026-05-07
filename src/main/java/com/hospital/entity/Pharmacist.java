package com.hospital.entity;

import jakarta.persistence.*;


/**
 * Pharmacist entity – Department 2.
 *
 * <p>pharmacistId format: PHA-YYYY-XXXX  (e.g. PHA-2024-0001)</p>
 */
@Entity
@Table(name = "pharmacists")
@DiscriminatorValue("PHARMACIST")
@PrimaryKeyJoinColumn(name = "user_id")
public class Pharmacist extends User {

    /** Human-readable identifier used on dispensing records. */
    @Column(unique = true, nullable = false, length = 20)
    private String pharmacistId;

    /** Pharmacy board license number. */
    @Column(nullable = false, length = 50)
    private String licenseNumber;
    
    // Explicit accessor to avoid Lombok-only reliance
    public String getPharmacistId() { return pharmacistId; }
    public void setPharmacistId(String pharmacistId) { this.pharmacistId = pharmacistId; }

    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }

    // No-arg constructor
    public Pharmacist() {
        super();
    }
}
