package com.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * PrescriptionFulfillment – records every dispense action by the pharmacy.
 *
 * <p>One prescription may have multiple fulfillment records if it has
 * refills.  The Finance department can query this table to include
 * medication dispensing costs in patient invoices.</p>
 */
@Entity
@Table(name = "prescription_fulfillments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PrescriptionFulfillment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pharmacist_id")
    private Pharmacist pharmacist;

    @Column(nullable = false)
    private int quantityDispensed;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime dispensedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FulfillmentStatus status = FulfillmentStatus.PENDING;

    /** Reason for rejection – populated only when status = REJECTED. */
    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 500)
    private String notes;

    // ── Status enum ──────────────────────────────────────────────────

    public enum FulfillmentStatus {
        PENDING,
        COMPLETED,
        REJECTED
    }

    // Explicit accessors for tools that don't run Lombok
    public Long getId() { return id; }
    public Prescription getPrescription() { return prescription; }
    public Pharmacist getPharmacist() { return pharmacist; }
    public int getQuantityDispensed() { return quantityDispensed; }
    public java.time.LocalDateTime getDispensedAt() { return dispensedAt; }
    public FulfillmentStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getNotes() { return notes; }
    public void setId(Long id) { this.id = id; }
    public void setPrescription(Prescription prescription) { this.prescription = prescription; }
    public void setPharmacist(Pharmacist pharmacist) { this.pharmacist = pharmacist; }
    public void setQuantityDispensed(int q) { this.quantityDispensed = q; }
    public void setStatus(FulfillmentStatus s) { this.status = s; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }
    public void setNotes(String n) { this.notes = n; }
}
