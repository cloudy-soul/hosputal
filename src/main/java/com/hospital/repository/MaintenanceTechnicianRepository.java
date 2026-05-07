package com.hospital.repository;

import com.hospital.entity.MaintenanceTechnician;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaintenanceTechnicianRepository extends JpaRepository<MaintenanceTechnician, Long> {

    Optional<MaintenanceTechnician> findByTechnicianId(String technicianId);

    Optional<MaintenanceTechnician> findByEmail(String email);

    boolean existsByTechnicianId(String technicianId);

    /** Returns all technicians currently flagged as on-call. */
    List<MaintenanceTechnician> findByIsOnCallTrue();

    /** Filters by area of expertise for smart assignment. */
    List<MaintenanceTechnician> findBySpecialization(String specialization);

    Page<MaintenanceTechnician> findAll(Pageable pageable);
}
