package com.hospital.repository;

import com.hospital.entity.SparePart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SparePartRepository extends JpaRepository<SparePart, Long> {

    Optional<SparePart> findByPartCode(String partCode);

    boolean existsByPartCode(String partCode);

    /** Parts at or below reorder threshold – triggers Purchase Department alert. */
    @Query("SELECT s FROM SparePart s WHERE s.stockQuantity <= s.reorderLevel")
    List<SparePart> findLowStockParts();

    List<SparePart> findByCategory(String category);

    Page<SparePart> findByPartNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("SELECT s FROM SparePart s WHERE " +
           "LOWER(s.partName) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "s.partCode LIKE CONCAT('%', :term, '%') OR " +
           "LOWER(s.category) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<SparePart> searchByKeyword(@Param("term") String term);
}
