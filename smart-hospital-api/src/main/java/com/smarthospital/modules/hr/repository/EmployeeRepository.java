package com.smarthospital.modules.hr.repository;

import com.smarthospital.modules.hr.domain.Employee;
import com.smarthospital.modules.hr.domain.Employee.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Page<Employee> findByDepartmentId(UUID departmentId, Pageable pageable);

    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCode(String code);

    @Query(value = "SELECT COUNT(*) + 1 FROM employees WHERE employee_code LIKE CONCAT('EMP-', :year, '-%')",
           nativeQuery = true)
    long nextSequenceForYear(@Param("year") int year);

    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(e.lastName)  LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(e.mobile)    LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(e.employeeCode) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Employee> search(@Param("q") String query, Pageable pageable);

    long countByStatus(EmployeeStatus status);

    List<Employee> findByDepartmentIdAndStatus(UUID departmentId, EmployeeStatus status);
}
