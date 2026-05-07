package com.hospital.repository;

import com.hospital.entity.Prescription;
import com.hospital.entity.PrescriptionFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionFulfillmentRepository extends JpaRepository<PrescriptionFulfillment, Long> {

    List<PrescriptionFulfillment> findByPrescription(Prescription prescription);
}
