package com.hospital.repository;

import com.hospital.entity.MedicationInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MedicationInventoryRepository extends JpaRepository<MedicationInventory, Long> {

    Optional<MedicationInventory> findByMedicationCode(String medicationCode);

    boolean existsByMedicationCode(String medicationCode);

    /** All medications below or at their reorder threshold. */
    @Query("SELECT m FROM MedicationInventory m WHERE m.currentStock <= m.reorderLevel")
    List<MedicationInventory> findLowStockItems();

    /** Search by name or code – used by pharmacy search endpoint. */
    @Query("SELECT m FROM MedicationInventory m WHERE " +
           "LOWER(m.medicationName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(m.genericName)    LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "m.medicationCode        LIKE CONCAT('%', :term, '%')")
    List<MedicationInventory> searchByNameOrCode(@Param("term") String term);
}
