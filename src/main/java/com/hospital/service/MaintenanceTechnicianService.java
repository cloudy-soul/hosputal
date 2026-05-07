package com.hospital.service;

import com.hospital.dto.RegisterTechnicianRequest;
import com.hospital.entity.User;
import com.hospital.entity.MaintenanceTechnician;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.UserRepository;
import com.hospital.repository.MaintenanceTechnicianRepository;
import com.hospital.service.AuditService;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maintenance Technician management service.
 *
 * <p>Handles registration, on-call toggling, and technician retrieval.
 * Reuses the same {@link IdGenerator} and {@link AuditService} as the
 * Doctor and Pharmacy departments for consistency.</p>
 */
@Service
@Transactional
public class MaintenanceTechnicianService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceTechnicianService.class);

    private final MaintenanceTechnicianRepository techRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final AuditService auditService;

    // Explicit constructor to avoid Lombok constructor reliance in IDE
    public MaintenanceTechnicianService(MaintenanceTechnicianRepository techRepo,
                                        UserRepository userRepo,
                                        PasswordEncoder passwordEncoder,
                                        IdGenerator idGenerator,
                                        AuditService auditService) {
        this.techRepo = techRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.idGenerator = idGenerator;
        this.auditService = auditService;
    }

    // ── Registration ─────────────────────────────────────────────────

    /**
     * Registers a new maintenance technician.
     * Requires ADMIN or MANAGER role (enforced at controller level).
     */
    public Map<String, Object> registerTechnician(RegisterTechnicianRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new BadRequestException("Email already registered: " + req.getEmail());
        }

        MaintenanceTechnician tech = new MaintenanceTechnician();
        tech.setEmail(req.getEmail());
        tech.setPassword(passwordEncoder.encode(req.getPassword()));
        tech.setFirstName(req.getFirstName());
        tech.setLastName(req.getLastName());
        tech.setRole(User.Role.MAINTENANCE);
        tech.setTechnicianId(generateUniqueTechnicianId());
        tech.setSpecialization(req.getSpecialization());
        tech.setCertificationNumber(req.getCertificationNumber());
        tech.setEmergencyContactPhone(req.getEmergencyContactPhone());
        tech.setOnCall(false);

        MaintenanceTechnician saved = techRepo.save(tech);

        auditService.log(saved.getId(), "REGISTER_TECHNICIAN", "MaintenanceTechnician", saved.getId(),
                "SYSTEM", saved.getTechnicianId() + " – " + saved.getSpecialization());

        log.info("Maintenance technician registered: {} ({})", saved.getTechnicianId(), saved.getSpecialization());
        return toTechnicianMap(saved);
    }

    // ── Retrieval ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getTechnicianById(Long id) {
        return toTechnicianMap(findById(id));
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAllTechnicians(Pageable pageable) {
        return techRepo.findAll(pageable).map(this::toTechnicianMap);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOnCallTechnicians() {
        return techRepo.findByIsOnCallTrue().stream().map(this::toTechnicianMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBySpecialization(String specialization) {
        return techRepo.findBySpecialization(specialization).stream().map(this::toTechnicianMap).toList();
    }

    // ── On-call management ───────────────────────────────────────────

    /**
     * Toggles the on-call status of a technician.
     * Called by MANAGER or ADMIN to set shift coverage.
     */
    public Map<String, Object> updateOnCallStatus(Long technicianId, boolean isOnCall) {
        MaintenanceTechnician tech = findById(technicianId);
        tech.setOnCall(isOnCall);
        techRepo.save(tech);
        log.info("Technician {} on-call status → {}", tech.getTechnicianId(), isOnCall);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("technicianId", tech.getTechnicianId());
        r.put("name", tech.getFullName());
        r.put("isOnCall", tech.isOnCall());
        r.put("message", "On-call status updated successfully.");
        return r;
    }

    /**
     * Auto-assigns the best available on-call technician for an EMERGENCY work order.
     * Returns the first on-call technician matching the required specialization if possible,
     * otherwise returns any on-call technician.
     */
    @Transactional(readOnly = true)
    public MaintenanceTechnician findBestAvailableTechnician(String preferredSpecialization) {
        // Try to match specialization first
        if (preferredSpecialization != null) {
            List<MaintenanceTechnician> specialists =
                techRepo.findBySpecialization(preferredSpecialization).stream()
                    .filter(MaintenanceTechnician::isOnCall).toList();
            if (!specialists.isEmpty()) return specialists.get(0);
        }
        // Fall back to any on-call technician
        List<MaintenanceTechnician> onCall = techRepo.findByIsOnCallTrue();
        if (onCall.isEmpty()) {
            throw new BadRequestException(
                "No on-call technicians available. Please assign manually.");
        }
        return onCall.get(0);
    }

    // ── Helpers ──────────────────────────────────────────────────────

    public MaintenanceTechnician findById(Long id) {
        return techRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + id));
    }

    public MaintenanceTechnician findByEmail(String email) {
        return techRepo.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + email));
    }

    private String generateUniqueTechnicianId() {
        String id;
        int attempts = 0;
        do {
            id = idGenerator.generateTechnicianId();
            if (++attempts > 20) throw new IllegalStateException("Could not generate unique technician ID");
        } while (techRepo.existsByTechnicianId(id));
        return id;
    }

    public Map<String, Object> toTechnicianMap(MaintenanceTechnician t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("technicianId", t.getTechnicianId());
        m.put("fullName", t.getFullName());
        m.put("email", t.getEmail());
        m.put("specialization", t.getSpecialization());
        m.put("certificationNumber", t.getCertificationNumber());
        m.put("isOnCall", t.isOnCall());
        m.put("emergencyContactPhone", t.getEmergencyContactPhone());
        m.put("role", t.getRole());
        return m;
    }
}
