package com.hospital.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/**
 * DTO for completing a preventive maintenance task.
 */
public class CompleteTaskRequest {
    @NotBlank
    private String report;
    private LocalDate completedOn;

    public CompleteTaskRequest() {
    }

    public String getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report = report;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(LocalDate completedOn) {
        this.completedOn = completedOn;
    }
}
 
