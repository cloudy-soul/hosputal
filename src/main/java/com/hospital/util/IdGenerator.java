package com.hospital.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates unique, human-readable identifiers for all hospital entities.
 *
 * <p>Formats:</p>
 * <ul>
 *   <li>Doctor:      DOC-YYYY-XXXX</li>
 *   <li>Pharmacist:  PHA-YYYY-XXXX</li>
 *   <li>Patient:     PAT-YYYY-XXXX</li>
 *   <li>Prescription:RX-YYYYMMDD-XXXX</li>
 *   <li>Appointment: APT-YYYYMMDD-XXXX</li>
 * </ul>
 *
 * <p>The counter resets per application restart; in production, replace
 * with a database sequence or UUID strategy for guaranteed uniqueness
 * across restarts.</p>
 */
@Component
public class IdGenerator {

    private final AtomicInteger counter = new AtomicInteger(1);

    private String nextSeq(int digits) {
        return String.format("%0" + digits + "d", counter.getAndIncrement());
    }

    public String generateDoctorId() {
        return "DOC-" + LocalDate.now().getYear() + "-" + nextSeq(4);
    }

    public String generatePharmacistId() {
        return "PHA-" + LocalDate.now().getYear() + "-" + nextSeq(4);
    }

    public String generatePatientId() {
        return "PAT-" + LocalDate.now().getYear() + "-" + nextSeq(4);
    }

    public String generatePrescriptionNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "RX-" + date + "-" + nextSeq(4);
    }

    public String generateAppointmentNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "APT-" + date + "-" + nextSeq(4);
    }

    // Maintenance-specific IDs
    public String generateTechnicianId() {
        return "MTC-" + LocalDate.now().getYear() + "-" + nextSeq(4);
    }

    public String generateWorkOrderNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "WO-" + date + "-" + nextSeq(4);
    }

    public String generateMaintenanceReportNumber() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "MR-" + date + "-" + nextSeq(4);
    }

    public String generateSparePartCode() {
        return "SP-" + LocalDate.now().getYear() + "-" + nextSeq(5);
    }

    public String generatePreventiveTaskCode() {
        return "PT-" + LocalDate.now().getYear() + "-" + nextSeq(4);
    }
}
