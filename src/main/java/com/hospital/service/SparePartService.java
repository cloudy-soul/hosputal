package com.hospital.service;

import com.hospital.dto.CreateSparePartRequest;
import com.hospital.dto.UpdateStockRequest;
import com.hospital.entity.SparePart;
import com.hospital.exception.BadRequestException;
import com.hospital.exception.ResourceNotFoundException;
import com.hospital.repository.SparePartRepository;
import com.hospital.service.AuditService;
import com.hospital.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spare Part Inventory Service.
 *
 * <p>Mirrors the Pharmacy Department's MedicationInventory service pattern:
 * same low-stock alert mechanism, same audit logging, same deduct-on-use pattern.
 * The Purchase Department (Phase 3) monitors {@code /api/maintenance/spare-parts/low-stock}
 * to trigger purchase orders.</p>
 */
@Service
@Transactional
public class SparePartService {

    private static final Logger log = LoggerFactory.getLogger(SparePartService.class);

    private final SparePartRepository sparePartRepo;
    private final AuditService        auditService;
    private final IdGenerator         idGenerator;

    public SparePartService(SparePartRepository sparePartRepo,
                            AuditService auditService,
                            IdGenerator idGenerator) {
        this.sparePartRepo = sparePartRepo;
        this.auditService = auditService;
        this.idGenerator = idGenerator;
    }

    // ── Create ───────────────────────────────────────────────────────

    public Map<String, Object> addSparePart(CreateSparePartRequest req, Long userId) {
        String partCode = (req.getPartCode() != null && !req.getPartCode().isBlank())
            ? req.getPartCode() : idGenerator.generateSparePartCode();

        if (sparePartRepo.existsByPartCode(partCode)) {
            throw new BadRequestException("Part code already exists: " + partCode);
        }

        SparePart part = new SparePart();
        part.setPartCode(partCode);
        part.setPartName(req.getPartName());
        part.setCategory(req.getCategory());
        part.setStockQuantity(req.getStockQuantity());
        part.setReorderLevel(req.getReorderLevel());
        part.setUnitPrice(req.getUnitPrice());
        part.setLocationShelf(req.getLocationShelf());
        part.setCompatibleEquipment(req.getCompatibleEquipment());

        SparePart saved = sparePartRepo.save(part);
        auditService.log(userId, "ADD_SPARE_PART", "SparePart", saved.getId(),
                "SYSTEM", saved.getPartCode() + " – " + saved.getPartName());

        return toPartMap(saved);
    }

    // ── Stock management ─────────────────────────────────────────────

    /**
     * Adjusts stock for a spare part.
     * Positive quantity = receive new stock. Negative = remove (damage/expiry).
     */
    public Map<String, Object> updateStock(Long partId, UpdateStockRequest req, Long userId) {
        SparePart part = findById(partId);
        int before = part.getStockQuantity();
        int newQty = before + req.getQuantity();

        if (newQty < 0) {
            throw new BadRequestException(
                "Adjustment would result in negative stock (" + newQty + ")");
        }

        part.setStockQuantity(newQty);
        sparePartRepo.save(part);

        auditService.log(userId, req.getQuantity() >= 0 ? "RECEIVE_SPARE_PART" : "ADJUST_SPARE_PART_STOCK",
                "SparePart", part.getId(), "SYSTEM",
                part.getPartCode() + " adj=" + req.getQuantity() +
                " reason=" + req.getReason() + " before=" + before + " after=" + newQty);

        Map<String, Object> r = toPartMap(part);
        r.put("previousStock", before);
        r.put("adjustment", req.getQuantity());
        r.put("reason", req.getReason());
        return r;
    }

    /**
     * Deducts stock when parts are used in a work order.
     * Called internally by {@link WorkOrderService} during work order completion.
     * Also available as a direct endpoint for ad-hoc deductions.
     */
    public void deductStock(Long partId, int quantity, Long workOrderId, Long userId) {
        SparePart part = findById(partId);

        if (part.hasInsufficientStock(quantity)) {
            throw new BadRequestException(
                "Insufficient stock for part " + part.getPartCode() +
                ": need " + quantity + ", have " + part.getStockQuantity());
        }

        int before = part.getStockQuantity();
        part.setStockQuantity(before - quantity);
        sparePartRepo.save(part);

        auditService.log(userId, "DEDUCT_SPARE_PART", "SparePart", part.getId(),
                "SYSTEM", part.getPartCode() + " qty=-" + quantity + " workOrder=" + workOrderId);

        if (part.isLowStock()) {
            log.warn("LOW STOCK ALERT: {} ({}) – {} units remaining (reorder: {})",
                part.getPartName(), part.getPartCode(),
                part.getStockQuantity(), part.getReorderLevel());
        }
    }

    // ── Queries ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLowStockParts() {
        return sparePartRepo.findLowStockParts().stream().map(this::toPartMap).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllParts() {
        return sparePartRepo.findAll().stream().map(this::toPartMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPartById(Long id) {
        return toPartMap(findById(id));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> searchParts(String keyword) {
        return sparePartRepo.searchByKeyword(keyword).stream().map(this::toPartMap).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    SparePart findById(Long id) {
        return sparePartRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Spare part not found: " + id));
    }

    Map<String, Object> toPartMap(SparePart p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("partCode", p.getPartCode());
        m.put("partName", p.getPartName());
        m.put("category", p.getCategory());
        m.put("stockQuantity", p.getStockQuantity());
        m.put("reorderLevel", p.getReorderLevel());
        m.put("isLowStock", p.isLowStock());
        m.put("unitPrice", p.getUnitPrice());
        m.put("locationShelf", p.getLocationShelf());
        m.put("compatibleEquipment", p.getCompatibleEquipment());
        m.put("lastUpdated", p.getLastUpdated());
        return m;
    }
}
