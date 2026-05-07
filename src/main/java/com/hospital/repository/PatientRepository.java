package com.hospital.repository;

import com.hospital.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);
    Optional<Patient> findByEmail(String email);

    /**
     * Full-name search used by Doctor Department's patient-search endpoint.
     * Case-insensitive, partial match on first or last name.
     */
    @Query("SELECT p FROM Patient p WHERE " +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(p.lastName)  LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "p.patientId        LIKE CONCAT('%', :term, '%')")
    List<Patient> searchByNameOrId(@Param("term") String term);
}
