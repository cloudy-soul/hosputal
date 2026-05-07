package com.hospital.service;

import com.hospital.entity.AuditLog;
import com.hospital.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Audit Service – compliance trail for all prescription and inventory actions.
 *
 * <p>Every CREATE_PRESCRIPTION, DISPENSE_MEDICATION, CANCEL_PRESCRIPTION,
 * RECEIVE_SHIPMENT, and ADJUST_STOCK call is logged here.
 * The Management and IT departments will query the audit_logs table for
 * security audits, regulatory inspections, and incident investigations.</p>
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(Long userId, String action, String entityType,
                    Long entityId, String ipAddress, String detail) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setTimestamp(LocalDateTime.now());
        entry.setIpAddress(ipAddress != null ? ipAddress : "SYSTEM");
        entry.setDetail(detail);
        auditLogRepository.save(entry);
        log.info("[AUDIT] user={} action={} entity={}#{} detail={}",
                userId, action, entityType, entityId, detail);
    }

    /** Convenience overload without detail. */
    public void log(Long userId, String action, String entityType, Long entityId) {
        log(userId, action, entityType, entityId, "SYSTEM", null);
    }

    // ── Predefined action constants ────────────────────────────────────

    public static final String CREATE_PRESCRIPTION  = "CREATE_PRESCRIPTION";
    public static final String CANCEL_PRESCRIPTION  = "CANCEL_PRESCRIPTION";
    public static final String DISPENSE_MEDICATION  = "DISPENSE_MEDICATION";
    public static final String REJECT_PRESCRIPTION  = "REJECT_PRESCRIPTION";
    public static final String COMPLETE_APPOINTMENT = "COMPLETE_APPOINTMENT";
    public static final String RECEIVE_SHIPMENT     = "RECEIVE_SHIPMENT";
    public static final String ADJUST_STOCK         = "ADJUST_STOCK";
    public static final String CREATE_APPOINTMENT   = "CREATE_APPOINTMENT";
}
