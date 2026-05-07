package com.hospital.dto;

import jakarta.validation.constraints.Min;

public class DispenseRequest {
    @Min(1) private int quantityDispensed;
    private String notes;

    public DispenseRequest() {}
    public int getQuantityDispensed() { return quantityDispensed; }
    public void setQuantityDispensed(int quantityDispensed) { this.quantityDispensed = quantityDispensed; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
