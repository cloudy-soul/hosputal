package com.hospital.dto;

import jakarta.validation.constraints.NotBlank;

/** CompleteAppointmentRequest DTO (moved out of combined file) */
public class CompleteAppointmentRequest {
    @NotBlank private String diagnosis;
    @NotBlank private String diagnosisSnomedCode;
    @NotBlank private String diagnosisSnomedDisplay;
    private String notes;
    private CreatePrescriptionRequest prescription;

    public CompleteAppointmentRequest() {}

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getDiagnosisSnomedCode() { return diagnosisSnomedCode; }
    public void setDiagnosisSnomedCode(String diagnosisSnomedCode) { this.diagnosisSnomedCode = diagnosisSnomedCode; }
    public String getDiagnosisSnomedDisplay() { return diagnosisSnomedDisplay; }
    public void setDiagnosisSnomedDisplay(String diagnosisSnomedDisplay) { this.diagnosisSnomedDisplay = diagnosisSnomedDisplay; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public CreatePrescriptionRequest getPrescription() { return prescription; }
    public void setPrescription(CreatePrescriptionRequest prescription) { this.prescription = prescription; }
}
