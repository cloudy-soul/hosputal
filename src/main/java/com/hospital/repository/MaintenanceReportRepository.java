package com.hospital.repository;

import com.hospital.entity.MaintenanceReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MaintenanceReportRepository extends JpaRepository<MaintenanceReport, Long> {

    Optional<MaintenanceReport> findByWorkOrderId(Long workOrderId);

    Optional<MaintenanceReport> findByReportNumber(String reportNumber);

    @Query("SELECT r FROM MaintenanceReport r WHERE r.technician.id = :technicianId ORDER BY r.completedAt DESC")
    List<MaintenanceReport> findByTechnicianId(@Param("technicianId") Long technicianId);

    /** Date-range query for Finance and Management reporting. */
    @Query("SELECT r FROM MaintenanceReport r WHERE r.completedAt BETWEEN :start AND :end ORDER BY r.completedAt DESC")
    List<MaintenanceReport> findByCompletedAtBetween(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    /** Aggregate hours per technician in a date range – used by HR payroll. */
    @Query("SELECT r.technician.id, SUM(r.hoursSpent) FROM MaintenanceReport r " +
           "WHERE r.completedAt BETWEEN :start AND :end GROUP BY r.technician.id")
    List<Object[]> sumHoursPerTechnicianBetween(@Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
