package com.hospital.entity;

import com.hospital.entity.User;
import jakarta.persistence.*;

/**
 * Maintenance Technician entity – Maintenance Department (Phase 2).
 *
 * <p>Extends {@link User} via JOINED inheritance, sharing the users table
 * for authentication/email/password while adding maintenance-specific columns
 * in the {@code maintenance_technicians} table.</p>
 *
 * <p>technicianId format: MTC-YYYY-XXXX (e.g. MTC-2025-0001)</p>
 *
 * <p><b>Integration hooks for other departments:</b></p>
 * <ul>
 *   <li>Any department (Doctor, Pharmacy, Nurse …) can create a {@link WorkOrder}
 *       referencing the reporting user – no coupling to this class needed.</li>
 *   <li>Finance: reads actual cost from {@link WorkOrder} and {@link MaintenanceReport}.</li>
 * </ul>
 */
@Entity
@Table(name = "maintenance_technicians")
@DiscriminatorValue("MAINTENANCE")
@PrimaryKeyJoinColumn(name = "user_id")
public class MaintenanceTechnician extends User {

    /** Human-readable unique identifier shown on work orders and reports. */
    @Column(unique = true, nullable = false, length = 20)
    private String technicianId;

    /**
     * Area of expertise.
     * Common values: HVAC, Electrical, Plumbing, Medical Equipment, Building, Generator
     */
    @Column(nullable = false, length = 80)
    private String specialization;

    /** Professional board certification / trade license number. */
    @Column(length = 60)
    private String certificationNumber;

    /**
     * Whether the technician is currently on-call for after-hours emergencies.
     * Updated via PUT /api/maintenance/technicians/{id}/on-call
     */
    @Column(nullable = false)
    private boolean isOnCall = false;

    /** Emergency contact phone – shown when an EMERGENCY work order is dispatched. */
    @Column(length = 30)
    private String emergencyContactPhone;

    // JPA no-arg constructor
    public MaintenanceTechnician() { super(); }

    // ---------- Explicit accessors (used by services/controllers) ----------
    public String getTechnicianId() { return technicianId; }
    public void setTechnicianId(String technicianId) { this.technicianId = technicianId; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getCertificationNumber() { return certificationNumber; }
    public void setCertificationNumber(String certificationNumber) { this.certificationNumber = certificationNumber; }

    public boolean isOnCall() { return isOnCall; }
    public void setOnCall(boolean onCall) { isOnCall = onCall; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
}
