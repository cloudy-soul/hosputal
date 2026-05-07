package com.hospital.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Base user entity shared across ALL 13 hospital departments.
 *
 * <p>Uses JOINED inheritance so that each department subclass (Doctor,
 * Pharmacist, Nurse, Finance, HR, …) gets its own table while still
 * sharing the common user data stored here.  Adding a new department
 * only requires creating a new @Entity that extends this class and
 * adding the corresponding role value to the {@link Role} enum.</p>
 *
 * <p>Current Phase 1 roles: DOCTOR, PHARMACIST, PATIENT, ADMIN.</p>
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt-hashed password – never store plain text. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 80)
    private String firstName;

    @Column(nullable = false, length = 80)
    private String lastName;

    /**
     * Role controls Spring Security authorization.
     *
     * <p>To integrate a new department, add the role here and create
     * the matching entity + security config.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Helper ──────────────────────────────────────────────────────

    /** Full name helper used in DTOs / response objects. */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Explicit accessors to avoid Lombok-only reliance in some tooling
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setRole(Role role) { this.role = role; }

    // Password and name accessors used throughout services
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    // ── Role enum ───────────────────────────────────────────────────

    /**
     * Add new values here as more departments are integrated.
     * e.g. NURSE, FINANCE, HR, MAINTENANCE, LOGISTICS, IT …
     */
    public enum Role {
        DOCTOR,
        PHARMACIST,
        PATIENT,
        ADMIN,
        // ── Future departments (Phase 2+) ──────────────────
        NURSE,
        FINANCE,
        HR,
        MANAGEMENT,
        HELP_DESK,
        PURCHASE,
        STORAGE,
        MAINTENANCE,
        IT,
        LOGISTICS,
        ACCOUNTING
    }
}
