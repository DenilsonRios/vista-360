package com.test_icesi.vista_360.academicrecord.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Oferta de una materia en un periodo concreto (grupo/sección). */
@Entity
@Table(name = "course_offering", uniqueConstraints = @UniqueConstraint(
        name = "uq_offering", columnNames = {"course_id", "term_id", "group_code"}))
@Getter
@Setter
@NoArgsConstructor
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private AcademicTerm term;

    @Column(name = "group_code", nullable = false)
    private String groupCode;

    @Column(name = "professor_name", nullable = false)
    private String professorName;
}
