package com.hospital.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/** CreatePrescriptionRequest DTO (moved out of the combined file) */
public class CreatePrescriptionRequest {
    @NotBlank private String patientId;
    @NotBlank private String medicationName;
    @NotBlank private String medicationCode;
    @NotBlank private String dosage;
    @NotBlank private String frequency;
    @Min(1)   private int duration;
    @Min(1)   private int quantity;
    private int refillsRemaining = 0;
    private String instructions;
    private boolean isControlledSubstance = false;
    private Long appointmentId;

    public CreatePrescriptionRequest() {}

    // getters/setters
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }
    public String getMedicationCode() { return medicationCode; }
    public void setMedicationCode(String medicationCode) { this.medicationCode = medicationCode; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getRefillsRemaining() { return refillsRemaining; }
    public void setRefillsRemaining(int refillsRemaining) { this.refillsRemaining = refillsRemaining; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public boolean isControlledSubstance() { return isControlledSubstance; }
    public void setControlledSubstance(boolean controlledSubstance) { isControlledSubstance = controlledSubstance; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
}
