package com.test_icesi.vista_360.academicrecord.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {
    Optional<AcademicTerm> findByCode(String code);

    Optional<AcademicTerm> findByCurrentTrue();
}
