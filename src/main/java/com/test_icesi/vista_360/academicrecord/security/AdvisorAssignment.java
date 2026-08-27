package com.test_icesi.vista_360.academicrecord.security;

import com.test_icesi.vista_360.academicrecord.domain.Student;

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

@Entity
@Table(name = "advisor_assignment", uniqueConstraints = @UniqueConstraint(
        name = "uq_assignment", columnNames = {"advisor_subject", "student_id"}))
@Getter
@Setter
@NoArgsConstructor
public class AdvisorAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "advisor_subject", nullable = false)
    private String advisorSubject;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private boolean active;
}
