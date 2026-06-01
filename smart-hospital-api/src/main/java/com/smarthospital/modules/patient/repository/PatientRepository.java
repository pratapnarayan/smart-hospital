package com.smarthospital.modules.patient.repository;

import com.smarthospital.modules.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    boolean existsByMobile(String mobile);

    /**
     * Full-text search via PostgreSQL tsvector, ordered by relevance rank.
     *
     * countQuery is mandatory for native paged queries: without it Hibernate wraps
     * the whole statement in a subquery which PostgreSQL rejects when ORDER BY is
     * present inside the subquery.
     *
     * Callers must pass an UNSORTED Pageable — the native SQL owns the ORDER BY.
     * Passing a sorted Pageable causes Spring Data to append a second ORDER BY
     * clause, which is a syntax error in PostgreSQL (SQLState 42601).
     */
    @Query(value = """
            SELECT * FROM patients
            WHERE deleted_at IS NULL
              AND search_vector @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
            """,
           countQuery = """
            SELECT COUNT(*) FROM patients
            WHERE deleted_at IS NULL
              AND search_vector @@ plainto_tsquery('english', :query)
            """,
           nativeQuery = true)
    Page<Patient> fullTextSearch(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE " +
           "(:query IS NULL OR LOWER(CONCAT(p.firstName, ' ', p.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR p.mobile LIKE CONCAT('%', :query, '%'))")
    Page<Patient> searchByNameOrMobile(@Param("query") String query, Pageable pageable);
}
