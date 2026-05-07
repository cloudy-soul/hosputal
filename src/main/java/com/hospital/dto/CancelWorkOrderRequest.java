package com.hospital.dto;

import jakarta.validation.constraints.*;

public class CancelWorkOrderRequest {
    @NotBlank
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
