package com.hospital.entity;

import com.hospital.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WorkOrder – the central entity of the Maintenance Department.
 *
 * <p>Any hospital employee (Doctor, Nurse, Pharmacist, Receptionist …)
 * can create a work order by referencing their User ID as {@code reportedBy}.
 * No dependency on any specific department entity is required.</p>
 *
 * <p>Work order number format: WO-YYYYMMDD-XXXX</p>
 *
 * <p><b>Integration hooks:</b></p>
 * <ul>
 *   <li>Finance: reads {@code estimatedCost} / {@code actualCost} for departmental budgeting.</li>
 *   <li>Management: reads counts by {@code status} and {@code priority} for KPI dashboards.</li>
 *   <li>IT: EMERGENCY work orders can trigger webhook notifications in Phase 3.</li>
 * </ul>
 */
@Entity
@Table(name = "work_orders")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique human-readable reference. Format: WO-YYYYMMDD-XXXX */
    @Column(unique = true, nullable = false, length = 25)
    private String workOrderNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    /**
     * The user who reported the issue.
     * Any User (regardless of role/department) can be a reporter.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedBy;

    /**
     * The technician assigned to fix the issue.
     * Null until a manager/admin assigns the order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_technician_id")
    private MaintenanceTechnician assignedTo;

    /** Name of the department that raised the issue. e.g. "Pharmacy", "Radiology". */
    @Column(length = 80)
    private String department;

    /** Physical location: room number, floor, wing. e.g. "Room 204", "Basement – Generator Room". */
    @Column(length = 150)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private WorkOrderStatus status = WorkOrderStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime reportedDate;

    /** Populated when status transitions to COMPLETED. */
    private LocalDateTime completedDate;

    /** Written by the technician when marking the order COMPLETED. */
    @Column(length = 1000)
    private String resolutionNotes;

    /** Cost estimate provided at assignment time (for Finance budgeting). */
    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedCost;

    /** Actual cost recorded at completion (for Finance invoicing). */
    @Column(precision = 10, scale = 2)
    private BigDecimal actualCost;

    // JPA no-arg constructor
    public WorkOrder() {}

    // ---------- Explicit accessors (a subset used by services) ----------
    public Long getId() { return id; }
    public String getWorkOrderNumber() { return workOrderNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public User getReportedBy() { return reportedBy; }
    public MaintenanceTechnician getAssignedTo() { return assignedTo; }
    public String getDepartment() { return department; }
    public String getLocation() { return location; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public LocalDateTime getReportedDate() { return reportedDate; }
    public LocalDateTime getCompletedDate() { return completedDate; }
    public String getResolutionNotes() { return resolutionNotes; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public BigDecimal getActualCost() { return actualCost; }

    public void setActualCost(BigDecimal actualCost) { this.actualCost = actualCost; }
    public void setCompletedDate(LocalDateTime completedDate) { this.completedDate = completedDate; }
    public void setStatus(WorkOrderStatus status) { this.status = status; }

    // Additional setters used by services/controllers
    public void setWorkOrderNumber(String workOrderNumber) { this.workOrderNumber = workOrderNumber; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setReportedBy(User reportedBy) { this.reportedBy = reportedBy; }
    public void setAssignedTo(MaintenanceTechnician assignedTo) { this.assignedTo = assignedTo; }
    public void setDepartment(String department) { this.department = department; }
    public void setLocation(String location) { this.location = location; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    // ── Enums ────────────────────────────────────────────────────────

    public enum Priority {
        LOW, MEDIUM, HIGH, EMERGENCY
    }

    public enum WorkOrderStatus {
        /** Submitted, not yet reviewed. */
        PENDING,
        /** Assigned to a technician, not yet started. */
        ASSIGNED,
        /** Technician is actively working on the issue. */
        IN_PROGRESS,
        /** Issue resolved; report generated. */
        COMPLETED,
        /** Cancelled by manager/admin with documented reason. */
        CANCELLED
    }
}
