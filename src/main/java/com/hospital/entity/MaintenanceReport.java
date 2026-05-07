package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MaintenanceReport – formal completion record for a work order.
 *
 * <p>Generated automatically when a technician marks a work order COMPLETED.
 * Serves as the auditable paper trail for JCI hospital accreditation and
 * Finance Department cost accounting.</p>
 *
 * <p>reportNumber format: MR-YYYYMMDD-XXXX</p>
 */
@Entity
@Table(name = "maintenance_reports")
public class MaintenanceReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 25)
    private String reportNumber;

    /** The work order this report closes out. One-to-one relationship. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technician_id")
    private MaintenanceTechnician technician;

    /** Narrative description of work performed. */
    @Column(nullable = false, length = 2000)
    private String workPerformed;

    /** Total labour hours billed for this job (for Finance payroll/billing). */
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal hoursSpent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime completedAt;

    /**
     * True when work was performed in a patient-accessible area
     * (requires a witness/department-head signature for compliance).
     */
    @Column(nullable = false)
    private boolean signatureRequired = false;

    /**
     * Name/ID of the department representative who signed off on the work.
     * Populated only when {@code signatureRequired = true}.
     */
    @Column(length = 150)
    private String patientAreaSignatory;

    // JPA requires a public or protected no-arg constructor
    public MaintenanceReport() {}

    // ---------- Explicit accessors used by services (added to avoid Lombok-only reliance) ----------
    public Long getId() { return id; }
    public String getReportNumber() { return reportNumber; }
    public void setReportNumber(String reportNumber) { this.reportNumber = reportNumber; }

    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }

    public MaintenanceTechnician getTechnician() { return technician; }
    public void setTechnician(MaintenanceTechnician technician) { this.technician = technician; }

    public String getWorkPerformed() { return workPerformed; }
    public void setWorkPerformed(String workPerformed) { this.workPerformed = workPerformed; }

    public BigDecimal getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(BigDecimal hoursSpent) { this.hoursSpent = hoursSpent; }

    public LocalDateTime getCompletedAt() { return completedAt; }

    public boolean isSignatureRequired() { return signatureRequired; }
    public void setSignatureRequired(boolean signatureRequired) { this.signatureRequired = signatureRequired; }

    public String getPatientAreaSignatory() { return patientAreaSignatory; }
    public void setPatientAreaSignatory(String patientAreaSignatory) { this.patientAreaSignatory = patientAreaSignatory; }
}
