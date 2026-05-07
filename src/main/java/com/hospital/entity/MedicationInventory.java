package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Medication Inventory entity – managed by the Pharmacy Department.
 *
 * <p>Uses {@code medicationCode} (RxNorm / NDC) as the canonical identifier
 * so it aligns with codes on {@link Prescription}.  The Purchase and
 * Storage departments (Phase 2) will extend stock management by
 * linking their order records to this entity via {@code medicationCode}.</p>
 */
@Entity
@Table(name = "medication_inventory")
public class MedicationInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RxNorm or NDC code – unique per medication.
     * Used to correlate with {@link Prescription#getMedicationCode()}.
     */
    @Column(unique = true, nullable = false, length = 30)
    private String medicationCode;

    @Column(nullable = false, length = 200)
    private String medicationName;

    /** International non-proprietary (generic) name. */
    @Column(length = 200)
    private String genericName;

    /** Strength e.g. "500mg", "10mg/5ml". */
    @Column(length = 50)
    private String strength;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MedicationForm form;

    @Column(length = 150)
    private String manufacturer;

    /** Current units in stock. Decremented on dispense. */
    @Column(nullable = false)
    private int currentStock;

    /**
     * Minimum threshold; a low-stock alert is raised when
     * {@code currentStock <= reorderLevel}.
     * Purchase department will act on low-stock events.
     */
    @Column(nullable = false)
    private int reorderLevel;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Physical shelf location in the pharmacy. */
    @Column(length = 50)
    private String locationShelf;

    /** True = valid prescription required before dispensing. */
    @Column(nullable = false)
    private boolean requiresPrescription = true;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    // ── Helper ───────────────────────────────────────────────────────

    /** Returns true when the stock is at or below the reorder threshold. */
    public boolean isLowStock() {
        return currentStock <= reorderLevel;
    }

    /** Returns true when there is insufficient stock to fill a quantity. */
    public boolean hasInsufficientStock(int required) {
        return currentStock < required;
    }

    // ── Form enum ────────────────────────────────────────────────────

    public enum MedicationForm {
        TABLET, CAPSULE, LIQUID, INJECTION, CREAM, PATCH, INHALER, DROPS
    }

    // Explicit accessors in case Lombok annotation processing isn't available
    public Long getId() { return id; }
    public String getMedicationCode() { return medicationCode; }
    public String getMedicationName() { return medicationName; }
    public String getGenericName() { return genericName; }
    public String getStrength() { return strength; }
    public MedicationForm getForm() { return form; }
    public String getManufacturer() { return manufacturer; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getReorderLevel() { return reorderLevel; }
    public java.math.BigDecimal getUnitPrice() { return unitPrice; }
    public String getLocationShelf() { return locationShelf; }
    public boolean isRequiresPrescription() { return requiresPrescription; }
    public java.time.LocalDateTime getLastUpdated() { return lastUpdated; }

    // Explicit setters for fields commonly set by DataLoader and services
    public void setMedicationCode(String medicationCode) { this.medicationCode = medicationCode; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public void setStrength(String strength) { this.strength = strength; }
    public void setForm(MedicationForm form) { this.form = form; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public void setUnitPrice(java.math.BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public void setLocationShelf(String locationShelf) { this.locationShelf = locationShelf; }
    public void setRequiresPrescription(boolean requiresPrescription) { this.requiresPrescription = requiresPrescription; }
    public void setId(Long id) { this.id = id; }
}
