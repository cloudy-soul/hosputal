package com.hospital.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * PreventiveMaintenanceTask – scheduled recurring maintenance for hospital equipment.
 *
 * <p>Critical for hospital accreditation (JCI / ISO 15189): medical equipment
 * must be serviced on documented schedules.  The {@code nextDueDate} is
 * recalculated automatically when a task is completed.</p>
 *
 * <p>taskCode format: PMT-YYYY-XXXX</p>
 */
@Entity
@Table(name = "preventive_maintenance_tasks")
public class PreventiveMaintenanceTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String taskCode;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String description;

    /**
     * Equipment type for grouping and filtering.
     * e.g. "X-Ray Machine", "HVAC System", "Emergency Generator", "Elevator", "Autoclave"
     */
    @Column(nullable = false, length = 100)
    private String equipmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MaintenanceFrequency frequency;

    // JPA no-arg constructor
    public PreventiveMaintenanceTask() {}

    // ---------- Explicit accessors used by services/controllers ----------
    public Long getId() { return id; }

    public String getTaskCode() { return taskCode; }
    public void setTaskCode(String taskCode) { this.taskCode = taskCode; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }

    public MaintenanceFrequency getFrequency() { return frequency; }
    public void setFrequency(MaintenanceFrequency frequency) { this.frequency = frequency; }

    public LocalDate getLastPerformedDate() { return lastPerformedDate; }
    public void setLastPerformedDate(LocalDate lastPerformedDate) { this.lastPerformedDate = lastPerformedDate; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public MaintenanceTechnician getAssignedTo() { return assignedTo; }
    public void setAssignedTo(MaintenanceTechnician assignedTo) { this.assignedTo = assignedTo; }

    public int getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }

    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    /** Date the task was last successfully completed. Null if never performed. */
    private LocalDate lastPerformedDate;

    /**
     * Next scheduled due date.
     * Recalculated from {@code frequency} when a task is completed.
     * Tasks where this date is past today appear in the overdue queue.
     */
    @Column(nullable = false)
    private LocalDate nextDueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private MaintenanceTechnician assignedTo;

    /** Estimated time to perform the task – used for workload planning. */
    private int estimatedDurationMinutes;

    /** Step-by-step instructions for the technician. */
    @Column(length = 1000)
    private String instructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TaskStatus status = TaskStatus.ACTIVE;

    // ── Enums ────────────────────────────────────────────────────────

    public enum MaintenanceFrequency {
        DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY;

        /** Calculates the next due date after a completion date. */
        public LocalDate nextFrom(LocalDate completedOn) {
            return switch (this) {
                case DAILY     -> completedOn.plusDays(1);
                case WEEKLY    -> completedOn.plusWeeks(1);
                case MONTHLY   -> completedOn.plusMonths(1);
                case QUARTERLY -> completedOn.plusMonths(3);
                case YEARLY    -> completedOn.plusYears(1);
            };
        }
    }

    public enum TaskStatus {
        ACTIVE,    // scheduled and running
        INACTIVE   // suspended (equipment decommissioned, etc.)
    }
}
