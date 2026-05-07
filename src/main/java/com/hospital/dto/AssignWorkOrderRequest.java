package com.hospital.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class AssignWorkOrderRequest {
    @NotNull
    private Long technicianId;
    private BigDecimal estimatedCost;

    public Long getTechnicianId() { return technicianId; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
}
