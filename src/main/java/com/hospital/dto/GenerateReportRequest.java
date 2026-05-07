package com.hospital.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class GenerateReportRequest {
    @NotBlank
    private String workPerformed;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal hoursSpent;

    private boolean signatureRequired = false;
    private String patientAreaSignatory;

    // Explicit getters/setters to avoid Lombok-only reliance in IDE
    public String getWorkPerformed() { return workPerformed; }
    public void setWorkPerformed(String workPerformed) { this.workPerformed = workPerformed; }

    public BigDecimal getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(BigDecimal hoursSpent) { this.hoursSpent = hoursSpent; }

    public boolean isSignatureRequired() { return signatureRequired; }
    public void setSignatureRequired(boolean signatureRequired) { this.signatureRequired = signatureRequired; }

    public String getPatientAreaSignatory() { return patientAreaSignatory; }
    public void setPatientAreaSignatory(String patientAreaSignatory) { this.patientAreaSignatory = patientAreaSignatory; }
}
