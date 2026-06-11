package com.smarthospital.modules.patient.repository;

import com.smarthospital.modules.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
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

    @Query(value = "SELECT COUNT(*) FROM patients WHERE deleted_at IS NULL", nativeQuery = true)
    long countTotal();

    @Query(value = "SELECT COUNT(*) FROM patients WHERE DATE(created_at) BETWEEN :from AND :to AND deleted_at IS NULL",
           nativeQuery = true)
    long countRegisteredBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT TO_CHAR(created_at, 'Mon YYYY'), COUNT(*) " +
                   "FROM patients WHERE DATE(created_at) BETWEEN :from AND :to AND deleted_at IS NULL " +
                   "GROUP BY TO_CHAR(created_at, 'Mon YYYY'), DATE_TRUNC('month', created_at) " +
                   "ORDER BY DATE_TRUNC('month', created_at)",
           nativeQuery = true)
    List<Object[]> countByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "SELECT gender, COUNT(*) FROM patients WHERE deleted_at IS NULL GROUP BY gender",
           nativeQuery = true)
    List<Object[]> countByGender();

    @Query(value = "SELECT blood_group, COUNT(*) FROM patients WHERE deleted_at IS NULL GROUP BY blood_group ORDER BY COUNT(*) DESC",
           nativeQuery = true)
    List<Object[]> countByBloodGroup();

    @Query(value = """
        SELECT
          CASE
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 0 AND 10 THEN '0-10'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 11 AND 20 THEN '11-20'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 21 AND 30 THEN '21-30'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 31 AND 40 THEN '31-40'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 41 AND 50 THEN '41-50'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 51 AND 60 THEN '51-60'
            WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) BETWEEN 61 AND 70 THEN '61-70'
            ELSE '71+'
          END AS age_bracket,
          COUNT(*) as cnt
        FROM patients WHERE deleted_at IS NULL AND date_of_birth IS NOT NULL
        GROUP BY age_bracket ORDER BY MIN(EXTRACT(YEAR FROM AGE(date_of_birth)))
        """, nativeQuery = true)
    List<Object[]> countByAgeBracket();
}
