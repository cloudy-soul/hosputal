package com.hospital.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class CompleteWorkOrderRequest {
    @NotBlank
    private String resolutionNotes;
    private BigDecimal actualCost;
    private BigDecimal hoursSpent;
    private boolean signatureRequired = false;
    private String patientAreaSignatory;
    /** Optional list of spare parts consumed during this job. */
    private List<PartUsageItem> partsUsed;

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public BigDecimal getActualCost() { return actualCost; }
    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }

    public BigDecimal getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(BigDecimal hoursSpent) { this.hoursSpent = hoursSpent; }

    public boolean isSignatureRequired() { return signatureRequired; }
    public void setSignatureRequired(boolean signatureRequired) { this.signatureRequired = signatureRequired; }

    public String getPatientAreaSignatory() { return patientAreaSignatory; }
    public void setPatientAreaSignatory(String patientAreaSignatory) { this.patientAreaSignatory = patientAreaSignatory; }

    public List<PartUsageItem> getPartsUsed() { return partsUsed; }
    public void setPartsUsed(List<PartUsageItem> partsUsed) { this.partsUsed = partsUsed; }

    public static class PartUsageItem {
        @NotBlank
        private String partCode;
        @Min(1)
        private int quantity;

        public String getPartCode() { return partCode; }
        public void setPartCode(String partCode) { this.partCode = partCode; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
