package com.hospital.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WorkOrderPartUsage – tracks which spare parts were consumed in a work order.
 *
 * <p>Records a price snapshot at {@code costAtTime} so historical cost reports
 * remain accurate even if the part's unit price changes later.</p>
 *
 * <p>Finance Department queries this table to calculate total material costs
 * per work order and per department for budget reconciliation.</p>
 */
@Entity
@Table(name = "work_order_part_usages")
public class WorkOrderPartUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id")
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "spare_part_id")
    private SparePart sparePart;

    @Column(nullable = false)
    private int quantityUsed;

    /**
     * Price snapshot at the time of use.
     * Ensures cost reports are historically accurate regardless of future price changes.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costAtTime;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;

    // Explicit accessors (replace Lombok)
    public Long getId() { return id; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public void setWorkOrder(WorkOrder workOrder) { this.workOrder = workOrder; }
    public SparePart getSparePart() { return sparePart; }
    public void setSparePart(SparePart sparePart) { this.sparePart = sparePart; }
    public int getQuantityUsed() { return quantityUsed; }
    public void setQuantityUsed(int quantityUsed) { this.quantityUsed = quantityUsed; }
    public BigDecimal getCostAtTime() { return costAtTime; }
    public void setCostAtTime(BigDecimal costAtTime) { this.costAtTime = costAtTime; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
