package com.test_icesi.vista_360.academicrecord.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.test_icesi.vista_360.academicrecord.domain.AcademicTerm;
import com.test_icesi.vista_360.academicrecord.domain.AcademicTermRepository;
import com.test_icesi.vista_360.academicrecord.domain.Course;
import com.test_icesi.vista_360.academicrecord.domain.CourseOffering;
import com.test_icesi.vista_360.academicrecord.domain.Enrollment;
import com.test_icesi.vista_360.academicrecord.domain.EnrollmentRepository;
import com.test_icesi.vista_360.academicrecord.domain.EnrollmentStatus;
import com.test_icesi.vista_360.academicrecord.domain.Program;
import com.test_icesi.vista_360.academicrecord.domain.Student;
import com.test_icesi.vista_360.academicrecord.domain.StudentRepository;
import com.test_icesi.vista_360.academicrecord.domain.StudentStatus;

@ExtendWith(MockitoExtension.class)
class AcademicRecordServiceTest {

    @Mock StudentRepository students;
    @Mock AcademicTermRepository terms;
    @Mock EnrollmentRepository enrollments;

    @InjectMocks AcademicRecordService service;

    @Test
    void returnsRecordForCurrentTermWhenTermNotProvided() {
        Student student = student("A00123456", "Laura", "Gómez", "Ingeniería de Sistemas");
        AcademicTerm term = term("2025-2", "Segundo semestre 2025", true);
        when(students.findByCode("A00123456")).thenReturn(Optional.of(student));
        when(terms.findByCurrentTrue()).thenReturn(Optional.of(term));
        when(enrollments.findAcademicRecord("A00123456", "2025-2"))
                .thenReturn(List.of(enrollment(student, term, "IS-101", "Intro", 3, "01",
                        EnrollmentStatus.ENROLLED, new BigDecimal("4.30"))));

        var response = service.getAcademicRecord("A00123456", null);

        assertThat(response.student().fullName()).isEqualTo("Laura Gómez");
        assertThat(response.term().code()).isEqualTo("2025-2");
        assertThat(response.enrolledCourses()).singleElement().satisfies(c -> {
            assertThat(c.courseCode()).isEqualTo("IS-101");
            assertThat(c.grade()).isEqualByComparingTo("4.30");
        });
    }

    @Test
    void usesExplicitTermWhenProvided() {
        Student student = student("A00123456", "Laura", "Gómez", "Ingeniería de Sistemas");
        AcademicTerm term = term("2025-1", "Primer semestre 2025", false);
        when(students.findByCode("A00123456")).thenReturn(Optional.of(student));
        when(terms.findByCode("2025-1")).thenReturn(Optional.of(term));
        when(enrollments.findAcademicRecord("A00123456", "2025-1")).thenReturn(List.of());

        var response = service.getAcademicRecord("A00123456", "2025-1");

        assertThat(response.term().code()).isEqualTo("2025-1");
        assertThat(response.enrolledCourses()).isEmpty();
    }

    @Test
    void throwsWhenStudentDoesNotExist() {
        when(students.findByCode("A00000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAcademicRecord("A00000000", null))
                .isInstanceOf(StudentNotFoundException.class);
    }

    @Test
    void throwsWhenExplicitTermDoesNotExist() {
        when(students.findByCode("A00123456"))
                .thenReturn(Optional.of(student("A00123456", "Laura", "Gómez", "IS")));
        when(terms.findByCode("2099-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAcademicRecord("A00123456", "2099-1"))
                .isInstanceOf(TermNotFoundException.class);
    }

    // --- helpers -------------------------------------------------------------

    private static Student student(String code, String first, String last, String program) {
        Program p = new Program();
        p.setName(program);
        p.setFaculty("Facultad");
        Student s = new Student();
        s.setCode(code);
        s.setDocument("doc-" + code);
        s.setFirstName(first);
        s.setLastName(last);
        s.setEmail(code + "@u.edu.co");
        s.setStatus(StudentStatus.ACTIVE);
        s.setProgram(p);
        return s;
    }

    private static AcademicTerm term(String code, String name, boolean current) {
        AcademicTerm t = new AcademicTerm();
        t.setCode(code);
        t.setName(name);
        t.setStartDate(LocalDate.now());
        t.setEndDate(LocalDate.now().plusMonths(4));
        t.setCurrent(current);
        return t;
    }

    private static Enrollment enrollment(Student student, AcademicTerm term, String courseCode,
            String courseName, int credits, String group, EnrollmentStatus status, BigDecimal grade) {
        Course course = new Course();
        course.setCode(courseCode);
        course.setName(courseName);
        course.setCredits(credits);
        CourseOffering offering = new CourseOffering();
        offering.setCourse(course);
        offering.setTerm(term);
        offering.setGroupCode(group);
        offering.setProfessorName("Profesor X");
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourseOffering(offering);
        e.setStatus(status);
        e.setEnrolledAt(Instant.now());
        e.setGrade(grade);
        return e;
    }
}
