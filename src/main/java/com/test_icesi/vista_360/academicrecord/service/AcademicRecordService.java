package com.test_icesi.vista_360.academicrecord.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.test_icesi.vista_360.academicrecord.api.dto.AcademicRecordResponse;
import com.test_icesi.vista_360.academicrecord.api.dto.EnrolledCourseResponse;
import com.test_icesi.vista_360.academicrecord.api.dto.StudentSummary;
import com.test_icesi.vista_360.academicrecord.api.dto.TermSummary;
import com.test_icesi.vista_360.academicrecord.domain.AcademicTerm;
import com.test_icesi.vista_360.academicrecord.domain.AcademicTermRepository;
import com.test_icesi.vista_360.academicrecord.domain.Enrollment;
import com.test_icesi.vista_360.academicrecord.domain.EnrollmentRepository;
import com.test_icesi.vista_360.academicrecord.domain.Student;
import com.test_icesi.vista_360.academicrecord.domain.StudentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AcademicRecordService {
    private final StudentRepository students;
    private final AcademicTermRepository terms;
    private final EnrollmentRepository enrollments;

    @Transactional(readOnly = true)
    public AcademicRecordResponse getAcademicRecord(String studentCode, String termCode) {
        Student student = students.findByCode(studentCode)
                .orElseThrow(() -> new StudentNotFoundException(studentCode));

        AcademicTerm term = StringUtils.hasText(termCode)
                ? terms.findByCode(termCode).orElseThrow(() -> new TermNotFoundException(termCode))
                : terms.findByCurrentTrue().orElseThrow(() -> new TermNotFoundException("vigente"));

        List<EnrolledCourseResponse> courses = enrollments
                .findAcademicRecord(student.getCode(), term.getCode())
                .stream()
                .map(AcademicRecordService::toCourse)
                .toList();

        return new AcademicRecordResponse(
                new StudentSummary(student.getCode(), student.fullName(), student.getProgram().getName()),
                new TermSummary(term.getCode(), term.getName()),
                courses);
    }

    private static EnrolledCourseResponse toCourse(Enrollment e) {
        var offering = e.getCourseOffering();
        var course = offering.getCourse();
        return new EnrolledCourseResponse(
                course.getCode(),
                course.getName(),
                course.getCredits(),
                offering.getGroupCode(),
                offering.getProfessorName(),
                e.getStatus().name(),
                e.getGrade());
    }
}
