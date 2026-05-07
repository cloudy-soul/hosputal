package com.hospital.repository;

import com.hospital.entity.Appointment;
import com.hospital.entity.Doctor;
import com.hospital.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorAndAppointmentDate(Doctor doctor, LocalDate date);

    List<Appointment> findByPatient(Patient patient);

    Optional<Appointment> findByAppointmentNumber(String appointmentNumber);

    boolean existsByAppointmentNumber(String appointmentNumber);
}
