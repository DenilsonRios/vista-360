package com.test_icesi.vista_360.academicrecord.api.dto;

import java.util.List;

/** Respuesta del servicio: estudiante, periodo consultado y materias inscritas con su nota. */
public record AcademicRecordResponse(
        StudentSummary student,
        TermSummary term,
        List<EnrolledCourseResponse> enrolledCourses) {
}
