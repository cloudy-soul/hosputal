package com.hospital.service;

import com.hospital.dto.CompleteTaskRequest;
import com.hospital.dto.CreatePreventiveTaskRequest;
import com.hospital.entity.MaintenanceTechnician;
import com.hospital.entity.PreventiveMaintenanceTask;
import com.hospital.entity.PreventiveMaintenanceTask.MaintenanceFrequency;
import com.hospital.entity.PreventiveMaintenanceTask.TaskStatus;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.MaintenanceTechnicianRepository;
import com.hospital.repository.PreventiveMaintenanceTaskRepository;
import com.hospital.service.AuditService;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Preventive Maintenance Task Service.
 *
 * <p>Manages the schedule of recurring maintenance tasks that are required
 * for hospital accreditation (JCI standard EP 2, 3) – e.g. weekly HVAC
 * filter checks, monthly X-Ray calibrations, quarterly generator tests.</p>
 *
 * <p>The {@code nextDueDate} is calculated automatically using the
 * {@link MaintenanceFrequency#nextFrom(LocalDate)} helper when a task is completed.</p>
 */
@Service
@Transactional
public class PreventiveMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PreventiveMaintenanceService.class);

    private final PreventiveMaintenanceTaskRepository taskRepo;
    private final MaintenanceTechnicianRepository techRepo;
    private final AuditService auditService;
    private final IdGenerator idGenerator;

    public PreventiveMaintenanceService(PreventiveMaintenanceTaskRepository taskRepo,
                                        MaintenanceTechnicianRepository techRepo,
                                        AuditService auditService,
                                        IdGenerator idGenerator) {
        this.taskRepo = taskRepo;
        this.techRepo = techRepo;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    // ── Create ───────────────────────────────────────────────────────

    public Map<String, Object> createTask(CreatePreventiveTaskRequest req, Long creatorId) {
        PreventiveMaintenanceTask task = new PreventiveMaintenanceTask();
        task.setTaskCode(idGenerator.generatePreventiveTaskCode());
        task.setTitle(req.getTitle());
        task.setDescription(req.getDescription());
        task.setEquipmentType(req.getEquipmentType());
        task.setFrequency(req.getFrequency());
        task.setNextDueDate(req.getNextDueDate());
        task.setEstimatedDurationMinutes(req.getEstimatedDurationMinutes());
        task.setInstructions(req.getInstructions());
        task.setStatus(TaskStatus.ACTIVE);

        if (req.getAssignedTechnicianId() != null) {
            MaintenanceTechnician tech = techRepo.findById(req.getAssignedTechnicianId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Technician not found: " + req.getAssignedTechnicianId()));
            task.setAssignedTo(tech);
        }

        PreventiveMaintenanceTask saved = taskRepo.save(task);
        auditService.log(creatorId, "CREATE_PREVENTIVE_TASK", "PreventiveMaintenanceTask",
                saved.getId(), "SYSTEM", saved.getTaskCode() + " – " + saved.getEquipmentType());

        log.info("Preventive task {} created: {} ({})", saved.getTaskCode(), saved.getTitle(), saved.getFrequency());
        return toTaskMap(saved);
    }

    // ── Complete ─────────────────────────────────────────────────────

    /**
     * Marks a preventive task as completed and advances the next due date.
     */
    public Map<String, Object> completeTask(Long taskId, CompleteTaskRequest req, Long technicianId) {
        PreventiveMaintenanceTask task = findById(taskId);

        if (task.getStatus() != TaskStatus.ACTIVE) {
            throw new BadRequestException("Task is not ACTIVE.");
        }

        LocalDate completedOn = req.getCompletedOn() != null ? req.getCompletedOn() : LocalDate.now();
        task.setLastPerformedDate(completedOn);
        task.setNextDueDate(task.getFrequency().nextFrom(completedOn));
        taskRepo.save(task);

        auditService.log(technicianId, "COMPLETE_PREVENTIVE_TASK", "PreventiveMaintenanceTask",
                task.getId(), "SYSTEM", task.getTaskCode() + " next=" + task.getNextDueDate());

        log.info("Preventive task {} completed. Next due: {}", task.getTaskCode(), task.getNextDueDate());
        return toTaskMap(task);
    }

    // ── Assign ───────────────────────────────────────────────────────

    public Map<String, Object> assignTaskToTechnician(Long taskId, Long technicianId, Long managerId) {
        PreventiveMaintenanceTask task = findById(taskId);
        MaintenanceTechnician tech = techRepo.findById(technicianId)
            .orElseThrow(() -> new ResourceNotFoundException("Technician not found: " + technicianId));

        task.setAssignedTo(tech);
        taskRepo.save(task);

        auditService.log(managerId, "ASSIGN_PREVENTIVE_TASK", "PreventiveMaintenanceTask",
                task.getId(), "SYSTEM", task.getTaskCode() + " → " + tech.getTechnicianId());

        return toTaskMap(task);
    }

    // ── Queries ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOverdueTasks() {
        return taskRepo.findAllOverdueTasks().stream().map(this::toTaskMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllTasks() {
        return taskRepo.findAll().stream().map(this::toTaskMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTaskById(Long id) {
        return toTaskMap(findById(id));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlySchedule(int year, int month) {
        return taskRepo.findByYearAndMonth(year, month).stream().map(t -> {
            Map<String, Object> m = toTaskMap(t);
            long daysUntilDue = LocalDate.now().until(t.getNextDueDate()).getDays();
            m.put("daysUntilDue", daysUntilDue);
            m.put("isOverdue", daysUntilDue < 0);
            return m;
        }).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    PreventiveMaintenanceTask findById(Long id) {
        return taskRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Preventive task not found: " + id));
    }

    Map<String, Object> toTaskMap(PreventiveMaintenanceTask t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("taskCode", t.getTaskCode());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("equipmentType", t.getEquipmentType());
        m.put("frequency", t.getFrequency());
        m.put("lastPerformedDate", t.getLastPerformedDate());
        m.put("nextDueDate", t.getNextDueDate());
        m.put("estimatedDurationMinutes", t.getEstimatedDurationMinutes());
        m.put("instructions", t.getInstructions());
        m.put("status", t.getStatus());
        m.put("isOverdue", t.getNextDueDate() != null && t.getNextDueDate().isBefore(LocalDate.now()));
        if (t.getAssignedTo() != null) {
            m.put("assignedTo", t.getAssignedTo().getFullName());
            m.put("assignedTechnicianId", t.getAssignedTo().getTechnicianId());
        }
        return m;
    }
}
