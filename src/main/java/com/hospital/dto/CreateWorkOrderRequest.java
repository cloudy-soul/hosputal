package com.hospital.dto;

import com.hospital.entity.WorkOrder.Priority;
import jakarta.validation.constraints.*;

public class CreateWorkOrderRequest {
    @NotBlank
    private String title;
    private String description;
    private String department;
    @NotBlank
    private String location;
    private Priority priority = Priority.MEDIUM;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
}
