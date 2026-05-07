package com.hospital.repository;

import com.hospital.entity.WorkOrder;
import com.hospital.entity.WorkOrder.Priority;
import com.hospital.entity.WorkOrder.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    Optional<WorkOrder> findByWorkOrderNumber(String workOrderNumber);

    boolean existsByWorkOrderNumber(String workOrderNumber);

    List<WorkOrder> findByStatus(WorkOrderStatus status);

    List<WorkOrder> findByPriorityAndStatus(Priority priority, WorkOrderStatus status);

    /** All non-completed orders assigned to a specific technician. */
    @Query("SELECT w FROM WorkOrder w WHERE w.assignedTo.id = :technicianId AND w.status = :status")
    List<WorkOrder> findByAssignedToIdAndStatus(@Param("technicianId") Long technicianId,
                                                @Param("status") WorkOrderStatus status);

    /** All orders submitted by a specific user (any department). */
    @Query("SELECT w FROM WorkOrder w WHERE w.reportedBy.id = :userId ORDER BY w.reportedDate DESC")
    Page<WorkOrder> findByReportedById(@Param("userId") Long userId, Pageable pageable);

    /** Active emergency orders – critical for real-time dashboard. */
    @Query("SELECT w FROM WorkOrder w WHERE w.priority = 'EMERGENCY' AND w.status <> 'COMPLETED' AND w.status <> 'CANCELLED' ORDER BY w.reportedDate ASC")
    List<WorkOrder> findActiveEmergencyOrders();

    /** Orders reported within a date range – used for Finance and Management reporting. */
    @Query("SELECT w FROM WorkOrder w WHERE w.reportedDate BETWEEN :start AND :end ORDER BY w.reportedDate DESC")
    List<WorkOrder> findByReportedDateBetween(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /** All orders assigned to a technician (paginated) for their personal dashboard. */
    @Query("SELECT w FROM WorkOrder w WHERE w.assignedTo.id = :technicianId ORDER BY w.reportedDate DESC")
    Page<WorkOrder> findAllByAssignedToId(@Param("technicianId") Long technicianId, Pageable pageable);

    long countByStatus(WorkOrderStatus status);

    long countByPriority(Priority priority);
}
