package com.hospital.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * AuditLog – compliance and traceability record.
 *
 * <p>Every prescription creation, dispense action, and inventory change
 * is recorded here.  Future departments (IT, Management, HR) can query
 * this table for access audits and disciplinary investigations.</p>
 *
 * <p>The table is append-only – no update/delete allowed at service layer.</p>
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who performed the action. */
    @Column(nullable = false)
    private Long userId;

    /**
     * Action type.
     * Examples: CREATE_PRESCRIPTION, DISPENSE_MEDICATION,
     *           CANCEL_PRESCRIPTION, RECEIVE_SHIPMENT, ADJUST_STOCK
     */
    @Column(nullable = false, length = 60)
    private String action;

    /** Entity type that was acted on e.g. "Prescription", "Appointment". */
    @Column(nullable = false, length = 60)
    private String entityType;

    /** Primary key of the affected entity. */
    @Column
    private Long entityId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** IP address of the request – set by AuditService from HttpServletRequest. */
    @Column(length = 45)  // IPv6 max length
    private String ipAddress;

    /** Optional free-text detail e.g. stock quantity before/after. */
    @Column(length = 500)
    private String detail;

    // No-arg constructor
    public AuditLog() {}

    // All-args constructor
    public AuditLog(Long id, Long userId, String action, String entityType, Long entityId, LocalDateTime timestamp, String ipAddress, String detail) {
        this.id = id;
        this.userId = userId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.detail = detail;
    }

    // Explicit getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
