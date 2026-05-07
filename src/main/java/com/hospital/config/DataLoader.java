package com.hospital.config;

import com.hospital.entity.*;
import com.hospital.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DataLoader – initialises the database with sample data on application startup.
 *
 * <p>Idempotent: checks for existence before inserting so re-runs are safe.</p>
 *
 * <p>Default credentials for Postman testing:</p>
 * <ul>
 *   <li>Doctor 1:      sarah.chen@hospital.com   / Doctor@123</li>
 *   <li>Doctor 2:      james.wilson@hospital.com / Doctor@123</li>
 *   <li>Pharmacist 1:  john.smith@hospital.com   / Pharma@123</li>
 *   <li>Pharmacist 2:  maria.garcia@hospital.com / Pharma@123</li>
 *   <li>Patient 1:     john.doe@example.com      / Patient@123</li>
 *   <li>Patient 2:     jane.smith@example.com    / Patient@123</li>
 * </ul>
 */
@Configuration
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepo;
    private final DoctorRepository doctorRepo;
    private final PharmacistRepository pharmacistRepo;
    private final PatientRepository patientRepo;
    private final AppointmentRepository appointmentRepo;
    private final MedicationInventoryRepository inventoryRepo;
    private final MaintenanceTechnicianRepository techRepo;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepo,
                      DoctorRepository doctorRepo,
                      PharmacistRepository pharmacistRepo,
                      PatientRepository patientRepo,
                      AppointmentRepository appointmentRepo,
                      MedicationInventoryRepository inventoryRepo,
                      MaintenanceTechnicianRepository techRepo,
                      PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.doctorRepo = doctorRepo;
        this.pharmacistRepo = pharmacistRepo;
        this.patientRepo = patientRepo;
        this.appointmentRepo = appointmentRepo;
        this.inventoryRepo = inventoryRepo;
        this.techRepo = techRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public CommandLineRunner loadData() {
        return args -> {
            log.info("DataLoader: seeding initial data...");

            // ── DOCTORS ──────────────────────────────────────────────

            Doctor dr1 = new Doctor();
            dr1.setEmail("sarah.chen@hospital.com");
            dr1.setPassword(passwordEncoder.encode("Doctor@123"));
            dr1.setFirstName("Sarah");
            dr1.setLastName("Chen");
            dr1.setRole(User.Role.DOCTOR);
            dr1.setDoctorId("DOC-2024-0001");
            dr1.setSpecialization("Cardiology");
            dr1.setLicenseNumber("MED-LIC-12345");
            dr1.setYearsOfExperience(12);
            if (!userRepo.existsByEmail(dr1.getEmail())) {
                doctorRepo.save(dr1);
            }

            Doctor dr2 = new Doctor();
            dr2.setEmail("james.wilson@hospital.com");
            dr2.setPassword(passwordEncoder.encode("Doctor@123"));
            dr2.setFirstName("James");
            dr2.setLastName("Wilson");
            dr2.setRole(User.Role.DOCTOR);
            dr2.setDoctorId("DOC-2024-0002");
            dr2.setSpecialization("Internal Medicine");
            dr2.setLicenseNumber("MED-LIC-67890");
            dr2.setYearsOfExperience(8);
            if (!userRepo.existsByEmail(dr2.getEmail())) {
                doctorRepo.save(dr2);
            }

            // ── PHARMACISTS ──────────────────────────────────────────

            Pharmacist ph1 = new Pharmacist();
            ph1.setEmail("john.smith@hospital.com");
            ph1.setPassword(passwordEncoder.encode("Pharma@123"));
            ph1.setFirstName("John");
            ph1.setLastName("Smith");
            ph1.setRole(User.Role.PHARMACIST);
            ph1.setPharmacistId("PHA-2024-0001");
            ph1.setLicenseNumber("PHA-LIC-11111");
            if (!userRepo.existsByEmail(ph1.getEmail())) {
                pharmacistRepo.save(ph1);
            }

            Pharmacist ph2 = new Pharmacist();
            ph2.setEmail("maria.garcia@hospital.com");
            ph2.setPassword(passwordEncoder.encode("Pharma@123"));
            ph2.setFirstName("Maria");
            ph2.setLastName("Garcia");
            ph2.setRole(User.Role.PHARMACIST);
            ph2.setPharmacistId("PHA-2024-0002");
            ph2.setLicenseNumber("PHA-LIC-22222");
            if (!userRepo.existsByEmail(ph2.getEmail())) {
                pharmacistRepo.save(ph2);
            }

            // ── PATIENTS ─────────────────────────────────────────────

            Patient p1 = new Patient();
            p1.setEmail("john.doe@example.com");
            p1.setPassword(passwordEncoder.encode("Patient@123"));
            p1.setFirstName("John");
            p1.setLastName("Doe");
            p1.setRole(User.Role.PATIENT);
            p1.setPatientId("PAT-2024-0001");
            p1.setDateOfBirth(LocalDate.of(1980, 6, 15));
            p1.setBloodGroup(Patient.BloodGroup.O_POS);
            p1.setAllergies("Penicillin");
            if (!userRepo.existsByEmail(p1.getEmail())) {
                patientRepo.save(p1);
            }

            Patient p2 = new Patient();
            p2.setEmail("jane.smith@example.com");
            p2.setPassword(passwordEncoder.encode("Patient@123"));
            p2.setFirstName("Jane");
            p2.setLastName("Smith");
            p2.setRole(User.Role.PATIENT);
            p2.setPatientId("PAT-2024-0002");
            p2.setDateOfBirth(LocalDate.of(1995, 3, 22));
            p2.setBloodGroup(Patient.BloodGroup.A_POS);
            p2.setAllergies(null);
            if (!userRepo.existsByEmail(p2.getEmail())) {
                patientRepo.save(p2);
            }

            Patient p3 = new Patient();
            p3.setEmail("ali.hassan@example.com");
            p3.setPassword(passwordEncoder.encode("Patient@123"));
            p3.setFirstName("Ali");
            p3.setLastName("Hassan");
            p3.setRole(User.Role.PATIENT);
            p3.setPatientId("PAT-2024-0003");
            p3.setDateOfBirth(LocalDate.of(1972, 11, 8));
            p3.setBloodGroup(Patient.BloodGroup.B_POS);
            p3.setAllergies("Sulfa,NSAIDs");
            if (!userRepo.existsByEmail(p3.getEmail())) {
                patientRepo.save(p3);
            }

            // ── ADMIN ────────────────────────────────────────────────

            User admin = new User();
            admin.setEmail("admin@hospital.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setFirstName("System");
            admin.setLastName("Admin");
            admin.setRole(User.Role.ADMIN);
            if (!userRepo.existsByEmail(admin.getEmail())) {
                userRepo.save(admin);
            }

            // ── MAINTENANCE ──────────────────────────────────────────

            MaintenanceTechnician tech1 = new MaintenanceTechnician();
            tech1.setEmail("tech.one@hospital.com");
            tech1.setPassword(passwordEncoder.encode("Tech@123"));
            tech1.setFirstName("Robert");
            tech1.setLastName("Miller");
            tech1.setRole(User.Role.MAINTENANCE);
            tech1.setTechnicianId("MTC-2024-0001");
            tech1.setSpecialization("Electrical");
            tech1.setCertificationNumber("ELEC-9988");
            tech1.setEmergencyContactPhone("555-0199");
            tech1.setOnCall(true);
            if (!userRepo.existsByEmail(tech1.getEmail())) {
                techRepo.save(tech1);
            }

            // ── SAMPLE APPOINTMENTS ──────────────────────────────────

            Appointment apt1 = new Appointment();
            apt1.setAppointmentNumber("APT-20240115-0001");
            apt1.setDoctor(dr1);
            apt1.setPatient(p1);
            apt1.setAppointmentDate(LocalDate.now());
            apt1.setAppointmentTime(LocalTime.of(9, 0));
            apt1.setStatus(Appointment.AppointmentStatus.SCHEDULED);
            apt1.setReasonForVisit("Routine hypertension follow-up");
            if (appointmentRepo.count() == 0) {
                appointmentRepo.save(apt1);
            }

            Appointment apt2 = new Appointment();
            apt2.setAppointmentNumber("APT-20240115-0002");
            apt2.setDoctor(dr2);
            apt2.setPatient(p2);
            apt2.setAppointmentDate(LocalDate.now());
            apt2.setAppointmentTime(LocalTime.of(10, 30));
            apt2.setStatus(Appointment.AppointmentStatus.SCHEDULED);
            apt2.setReasonForVisit("Diabetes management review");
            if (appointmentRepo.count() <= 1) {
                appointmentRepo.save(apt2);
            }

            // ── MEDICATION INVENTORY ─────────────────────────────────
            // RxNorm codes used as medicationCode for simplicity

            saveInventory("0001", "Lisinopril 10mg",   "Lisinopril",   "10mg",  500, 50,  new BigDecimal("0.15"), "A1", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0002", "Metformin 500mg",   "Metformin",    "500mg", 300, 50,  new BigDecimal("0.08"), "A2", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0003", "Amoxicillin 500mg", "Amoxicillin",  "500mg", 0,   50,  new BigDecimal("0.12"), "B1", MedicationInventory.MedicationForm.CAPSULE);
            saveInventory("0004", "Atorvastatin 20mg", "Atorvastatin", "20mg",  200, 30,  new BigDecimal("0.22"), "B2", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0005", "Levothyroxine 50mcg","Levothyroxine","50mcg",150, 30,  new BigDecimal("0.18"), "C1", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0006", "Warfarin 5mg",      "Warfarin",     "5mg",   120, 40,  new BigDecimal("0.25"), "C2", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0007", "Aspirin 100mg",     "Aspirin",      "100mg", 600, 100, new BigDecimal("0.05"), "D1", MedicationInventory.MedicationForm.TABLET);
            saveInventory("0008", "Omeprazole 20mg",   "Omeprazole",   "20mg",  250, 60,  new BigDecimal("0.10"), "D2", MedicationInventory.MedicationForm.CAPSULE);
            saveInventory("0009", "Amoxicillin 250mg", "Amoxicillin",  "250mg", 35,  50,  new BigDecimal("0.09"), "B3", MedicationInventory.MedicationForm.CAPSULE); // intentionally low-stock
            saveInventory("0010", "Salbutamol Inhaler","Salbutamol",   "100mcg/dose", 80, 20, new BigDecimal("4.50"), "E1", MedicationInventory.MedicationForm.INHALER);

            log.info("DataLoader: seeding complete.");
            log.info("──────────────────────────────────────────────────");
            log.info("Test credentials:");
            log.info("  Doctor:     sarah.chen@hospital.com  / Doctor@123");
            log.info("  Pharmacist: john.smith@hospital.com  / Pharma@123");
            log.info("  Patient:    john.doe@example.com     / Patient@123");
            log.info("  Admin:      admin@hospital.com       / Admin@123");
            log.info("  Maintenance:tech.one@hospital.com    / Tech@123");
            log.info("──────────────────────────────────────────────────");
        };
    }

    private void saveInventory(String code, String name, String generic, String strength,
                                int stock, int reorder, BigDecimal price, String shelf,
                                MedicationInventory.MedicationForm form) {
        if (inventoryRepo.existsByMedicationCode(code)) return;

        MedicationInventory inv = new MedicationInventory();
        inv.setMedicationCode(code);
        inv.setMedicationName(name);
        inv.setGenericName(generic);
        inv.setStrength(strength);
        inv.setForm(form);
        inv.setManufacturer("Generic Pharma Co.");
        inv.setCurrentStock(stock);
        inv.setReorderLevel(reorder);
        inv.setUnitPrice(price);
        inv.setLocationShelf(shelf);
        inv.setRequiresPrescription(true);
        inventoryRepo.save(inv);
    }
}
