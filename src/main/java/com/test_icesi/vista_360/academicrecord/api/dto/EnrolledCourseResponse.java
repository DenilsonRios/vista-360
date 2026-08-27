package com.test_icesi.vista_360.academicrecord.api.dto;

import java.math.BigDecimal;

public record EnrolledCourseResponse(
        String courseCode,
        String courseName,
        int credits,
        String group,
        String professor,
        String status,
        BigDecimal grade) {
}
