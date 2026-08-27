package com.test_icesi.vista_360.academicrecord.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class EnrollmentRepositoryTest {
    @Autowired
    EnrollmentRepository enrollments;

    @Test
    void returnsEnrolledCoursesForCurrentTermExcludingWithdrawn() {
        List<Enrollment> record = enrollments.findAcademicRecord("A00123456", "2025-2");

        assertThat(record).hasSize(3);
        assertThat(record).extracting(e -> e.getCourseOffering().getCourse().getCode())
                .containsExactly("IS-101", "IS-205", "MAT-201");
        assertThat(record).allSatisfy(e ->
                assertThat(e.getStatus()).isNotEqualTo(EnrollmentStatus.WITHDRAWN));
    }

    @Test
    void doesNotMixOtherTerms() {
        List<Enrollment> record = enrollments.findAcademicRecord("A00123456", "2025-1");

        assertThat(record).extracting(e -> e.getCourseOffering().getCourse().getCode())
                .containsExactly("IS-101");
    }

    @Test
    void withdrawnEnrollmentIsExcluded() {
        List<Enrollment> record = enrollments.findAcademicRecord("A00111222", "2025-2");

        assertThat(record).isEmpty();
    }
}
