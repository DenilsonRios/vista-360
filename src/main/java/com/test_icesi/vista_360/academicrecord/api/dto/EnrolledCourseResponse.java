package com.test_icesi.vista_360.academicrecord.api.dto;

import java.math.BigDecimal;

/** Materia inscrita por el estudiante en el periodo consultado. */
public record EnrolledCourseResponse(
        String courseCode,
        String courseName,
        int credits,
        String group,
        String professor,
        String status,
        /** Nota a la fecha; {@code null} si la materia aún no tiene calificación. */
        BigDecimal grade) {
}
