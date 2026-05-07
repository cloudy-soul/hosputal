package com.hospital.repository;

import com.hospital.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Optional<Doctor> findByDoctorId(String doctorId);
    Optional<Doctor> findByEmail(String email);
    boolean existsByDoctorId(String doctorId);
}
