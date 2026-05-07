package com.hospital.repository;

import com.hospital.entity.PreventiveMaintenanceTask;
import com.hospital.entity.PreventiveMaintenanceTask.MaintenanceFrequency;
import com.hospital.entity.PreventiveMaintenanceTask.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PreventiveMaintenanceTaskRepository extends JpaRepository<PreventiveMaintenanceTask, Long> {

    Optional<PreventiveMaintenanceTask> findByTaskCode(String taskCode);

    boolean existsByTaskCode(String taskCode);

    /** Tasks overdue as of a given date (typically today). */
    List<PreventiveMaintenanceTask> findByNextDueDateBeforeAndStatus(LocalDate date, TaskStatus status);

    List<PreventiveMaintenanceTask> findByFrequency(MaintenanceFrequency frequency);

    @Query("SELECT p FROM PreventiveMaintenanceTask p WHERE p.assignedTo.id = :technicianId")
    List<PreventiveMaintenanceTask> findByAssignedToId(@Param("technicianId") Long technicianId);

    /** Tasks due in a specific month and year – generates the monthly schedule. */
    @Query("SELECT p FROM PreventiveMaintenanceTask p " +
           "WHERE YEAR(p.nextDueDate) = :year AND MONTH(p.nextDueDate) = :month " +
           "AND p.status = 'ACTIVE' ORDER BY p.nextDueDate ASC")
    List<PreventiveMaintenanceTask> findByYearAndMonth(@Param("year") int year,
                                                       @Param("month") int month);

    /** Currently active overdue tasks – used by dashboard alert. */
    @Query("SELECT p FROM PreventiveMaintenanceTask p " +
           "WHERE p.nextDueDate <= CURRENT_DATE AND p.status = 'ACTIVE'")
    List<PreventiveMaintenanceTask> findAllOverdueTasks();
}
