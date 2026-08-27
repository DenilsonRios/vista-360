package com.test_icesi.vista_360.academicrecord.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Materias que el estudiante tiene inscritas en un periodo (excluye las canceladas),
     * con la oferta, la materia y el periodo ya cargados para evitar N+1.
     */
    @Query("""
            select e from Enrollment e
              join fetch e.courseOffering o
              join fetch o.course c
              join fetch o.term t
            where e.student.code = :studentCode
              and t.code = :termCode
              and e.status <> com.test_icesi.vista_360.academicrecord.domain.EnrollmentStatus.WITHDRAWN
            order by c.code
            """)
    List<Enrollment> findAcademicRecord(@Param("studentCode") String studentCode,
                                        @Param("termCode") String termCode);
}
