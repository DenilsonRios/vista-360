package com.test_icesi.vista_360.academicrecord.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock
    AdvisorAssignmentRepository assignments;

    @InjectMocks
    AccessControlService accessControlService;

    private static Jwt jwt(String subject, List<String> roles, String studentId) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", roles);
        if (studentId != null) {
            builder.claim("student_id", studentId);
        }
        return builder.build();
    }

    @Test
    void studentCanSeeOwnRecord() {
        Jwt token = jwt("student-1", List.of("STUDENT"), "A00123456");

        assertThatCode(() -> accessControlService.checkCanViewStudent("A00123456", token))
                .doesNotThrowAnyException();
    }

    @Test
    void studentCannotSeeAnotherStudent() {
        Jwt token = jwt("student-1", List.of("STUDENT"), "A00123456");

        assertThatThrownBy(() -> accessControlService.checkCanViewStudent("A00999999", token))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void advisorCanSeeAssignedStudent() {
        Jwt token = jwt("advisor-001", List.of("ADVISOR"), null);
        when(assignments.existsByAdvisorSubjectAndStudent_CodeAndActiveTrue("advisor-001", "A00123456"))
                .thenReturn(true);

        assertThatCode(() -> accessControlService.checkCanViewStudent("A00123456", token))
                .doesNotThrowAnyException();
    }

    @Test
    void advisorCannotSeeUnassignedStudent() {
        Jwt token = jwt("advisor-001", List.of("ADVISOR"), null);
        when(assignments.existsByAdvisorSubjectAndStudent_CodeAndActiveTrue("advisor-001", "A00987654"))
                .thenReturn(false);

        assertThatThrownBy(() -> accessControlService.checkCanViewStudent("A00987654", token))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void tokenWithoutRelevantRoleIsRejected() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "HS256").subject("x")
                .claims(c -> c.putAll(Map.of("scope", "read")))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();

        assertThatThrownBy(() -> accessControlService.checkCanViewStudent("A00123456", token))
                .isInstanceOf(AccessDeniedException.class);
    }
}
