package com.hospital.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectPrescriptionRequest {
    @NotBlank private String rejectionReason;
    public RejectPrescriptionRequest() {}
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
