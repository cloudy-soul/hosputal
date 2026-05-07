package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SparePart – inventory of components used in maintenance work orders.
 *
 * <p>Mirrors the same low-stock-alert pattern used by {@code MedicationInventory}
 * in the Pharmacy Department, allowing the Purchase Department (Phase 3) to
 * apply one unified reorder workflow across both medication and spare-part stock.</p>
 *
 * <p>partCode format: SP-YYYY-XXXX</p>
 *
 * <p><b>Integration hooks:</b></p>
 * <ul>
 *   <li>Finance: reads {@code unitPrice} and {@link WorkOrderPartUsage} for cost allocation.</li>
 *   <li>Purchase: monitors {@code isLowStock()} to trigger purchase orders.</li>
 *   <li>Storage: updates {@code stockQuantity} when shipments arrive.</li>
 * </ul>
 */
@Entity
@Table(name = "spare_parts")
public class SparePart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String partCode;

    @Column(nullable = false, length = 200)
    private String partName;

    /**
     * Category for filtering and reporting.
     * e.g. ELECTRICAL, MECHANICAL, FILTERS, LUBRICANTS, CONSUMABLES, MEDICAL_EQUIPMENT
     */
    @Column(nullable = false, length = 60)
    private String category;

    /** Current units in stock. Decremented when parts are used in work orders. */
    @Column(nullable = false)
    private int stockQuantity;

    /**
     * Minimum stock before a low-stock alert is triggered.
     * Purchase Department acts on alerts from {@code /api/maintenance/spare-parts/low-stock}.
     */
    @Column(nullable = false)
    private int reorderLevel;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /** Physical shelf in the maintenance store room. e.g. "S3-B2". */
    @Column(length = 50)
    private String locationShelf;

    /**
     * Comma-separated list of equipment models this part fits.
     * e.g. "Siemens X-Ray MULTIX, GE Optima XR"
     */
    @Column(length = 500)
    private String compatibleEquipment;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    // ── Helper ───────────────────────────────────────────────────────

    public boolean isLowStock() {
        return stockQuantity <= reorderLevel;
    }

    public boolean hasInsufficientStock(int required) {
        return stockQuantity < required;
    }

    // Explicit accessors (replace Lombok-generated methods)
    public Long getId() { return id; }
    public String getPartCode() { return partCode; }
    public void setPartCode(String partCode) { this.partCode = partCode; }
    public String getPartName() { return partName; }
    public void setPartName(String partName) { this.partName = partName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public String getLocationShelf() { return locationShelf; }
    public void setLocationShelf(String locationShelf) { this.locationShelf = locationShelf; }
    public String getCompatibleEquipment() { return compatibleEquipment; }
    public void setCompatibleEquipment(String compatibleEquipment) { this.compatibleEquipment = compatibleEquipment; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
