package com.hospital.dto;

import jakarta.validation.constraints.*;

public class UpdateWorkOrderStatusRequest {
    @NotNull
    private String status;   // WorkOrderStatus name
    private String notes;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
