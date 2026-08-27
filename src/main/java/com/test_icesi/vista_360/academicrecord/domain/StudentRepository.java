package com.test_icesi.vista_360.academicrecord.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @EntityGraph(attributePaths = "program")
    Optional<Student> findByCode(String code);
}
