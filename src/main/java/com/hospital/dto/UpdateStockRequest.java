package com.hospital.dto;

import jakarta.validation.constraints.*;

public class UpdateStockRequest {
    private int quantity;          // positive = receive, negative = remove
    @NotBlank
    private String reason;

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
