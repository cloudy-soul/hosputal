package com.hospital.repository;

import com.hospital.entity.Pharmacist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PharmacistRepository extends JpaRepository<Pharmacist, Long> {
    Optional<Pharmacist> findByPharmacistId(String pharmacistId);
    Optional<Pharmacist> findByEmail(String email);
}
