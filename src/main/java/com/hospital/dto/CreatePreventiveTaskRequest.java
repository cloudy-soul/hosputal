package com.hospital.dto;

import com.hospital.entity.PreventiveMaintenanceTask.MaintenanceFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO for creating a preventive maintenance task.
 */
public class CreatePreventiveTaskRequest {
    @NotBlank
    private String title;
    private String description;
    @NotBlank
    private String equipmentType;
    @NotNull
    private MaintenanceFrequency frequency;
    @NotNull
    private LocalDate nextDueDate;
    private int estimatedDurationMinutes;
    private String instructions;
    private Long assignedTechnicianId;

    public CreatePreventiveTaskRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public MaintenanceFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(MaintenanceFrequency frequency) {
        this.frequency = frequency;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public Long getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Long assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }
}
 
