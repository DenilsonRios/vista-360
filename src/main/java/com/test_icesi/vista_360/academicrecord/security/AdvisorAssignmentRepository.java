package com.test_icesi.vista_360.academicrecord.security;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvisorAssignmentRepository extends JpaRepository<AdvisorAssignment, Long> {

    boolean existsByAdvisorSubjectAndStudent_CodeAndActiveTrue(String advisorSubject, String studentCode);
}
