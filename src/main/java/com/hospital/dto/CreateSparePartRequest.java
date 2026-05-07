package com.hospital.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class CreateSparePartRequest {
    private String partCode;                  // auto-generated if blank
    @NotBlank
    private String partName;
    @NotBlank
    private String category;
    @Min(0)
    private int stockQuantity;
    @Min(1)
    private int reorderLevel;
    private BigDecimal unitPrice;
    private String locationShelf;
    private String compatibleEquipment;

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
}
