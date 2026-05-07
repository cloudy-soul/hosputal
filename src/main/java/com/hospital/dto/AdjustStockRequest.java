package com.hospital.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to adjust stock for a medication (positive to add, negative to remove).
 */
public class AdjustStockRequest {
    @NotBlank
    private String medicationCode;
    private int adjustment; // positive = add, negative = remove
    @NotBlank
    private String reason;

    public AdjustStockRequest() {}

    public String getMedicationCode() {
        return medicationCode;
    }

    public void setMedicationCode(String medicationCode) {
        this.medicationCode = medicationCode;
    }

    public int getAdjustment() {
        return adjustment;
    }

    public void setAdjustment(int adjustment) {
        this.adjustment = adjustment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
