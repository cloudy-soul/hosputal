package com.hospital.repository;

import com.hospital.entity.Patient;
import com.hospital.entity.Prescription;
import com.hospital.entity.Prescription.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByPrescriptionNumber(String prescriptionNumber);

    boolean existsByPrescriptionNumber(String prescriptionNumber);

    List<Prescription> findByPatientAndStatus(Patient patient, PrescriptionStatus status);

    List<Prescription> findByStatus(PrescriptionStatus status);

    /** Full medical history for a patient – all statuses, newest first. */
    @Query("SELECT p FROM Prescription p WHERE p.patient = :patient ORDER BY p.prescribedDate DESC")
    List<Prescription> findAllByPatientOrderByDateDesc(@Param("patient") Patient patient);

    /** Count pending prescriptions – used by pharmacy dashboard. */
    long countByStatus(PrescriptionStatus status);
}
