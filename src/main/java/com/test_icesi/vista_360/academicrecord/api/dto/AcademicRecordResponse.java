package com.test_icesi.vista_360.academicrecord.api.dto;

import java.util.List;

public record AcademicRecordResponse(
        StudentSummary student,
        TermSummary term,
        List<EnrolledCourseResponse> enrolledCourses) {
}
