package com.hospital.repository;

import com.hospital.entity.WorkOrderPartUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkOrderPartUsageRepository extends JpaRepository<WorkOrderPartUsage, Long> {

    List<WorkOrderPartUsage> findByWorkOrderId(Long workOrderId);

    List<WorkOrderPartUsage> findBySparePartId(Long sparePartId);

    /** Cost summary per work order – used by Finance for cost allocation. */
    @Query("SELECT u FROM WorkOrderPartUsage u WHERE u.recordedAt BETWEEN :start AND :end")
    List<WorkOrderPartUsage> findByRecordedAtBetween(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);
}
