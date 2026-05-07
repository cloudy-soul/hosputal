package com.hospital.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public class ReceiveShipmentRequest {
    @NotBlank private String medicationCode;
    @Min(1) private int quantity;
    private String batchNumber;
    private LocalDate expiryDate;
    private String notes;

    public ReceiveShipmentRequest() {}
    public String getMedicationCode() { return medicationCode; }
    public void setMedicationCode(String medicationCode) { this.medicationCode = medicationCode; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
