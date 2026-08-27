package com.test_icesi.vista_360.academicrecord.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Autorización a nivel de recurso para la consulta del registro académico.
 *
 * <ul>
 *   <li>Un <b>estudiante</b> solo puede ver su propia información: el {@code studentCode}
 *       de la ruta debe coincidir con el claim {@code student_id} de su token.</li>
 *   <li>Un <b>asesor de acompañamiento</b> solo puede ver a los estudiantes que tiene
 *       asignados (tabla {@code advisor_assignment}).</li>
 * </ul>
 *
 * La regla se evalúa en el backend con el identificador del token; nunca se confía en
 * un identificador enviado por el cliente.
 */
@Service
@RequiredArgsConstructor
public class AccessControlService {

    static final String ROLE_STUDENT = "STUDENT";
    static final String ROLE_ADVISOR = "ADVISOR";
    static final String CLAIM_ROLES = "roles";
    static final String CLAIM_STUDENT_ID = "student_id";

    private final AdvisorAssignmentRepository assignments;

    public void checkCanViewStudent(String studentCode, Jwt token) {
        List<String> roles = rolesOf(token);

        if (roles.contains(ROLE_STUDENT)
                && studentCode.equals(token.getClaimAsString(CLAIM_STUDENT_ID))) {
            return;
        }
        if (roles.contains(ROLE_ADVISOR)
                && assignments.existsByAdvisorSubjectAndStudent_CodeAndActiveTrue(
                        token.getSubject(), studentCode)) {
            return;
        }
        throw new AccessDeniedException(
                "El usuario no está autorizado para consultar la información de este estudiante");
    }

    private static List<String> rolesOf(Jwt token) {
        Object claim = token.getClaim(CLAIM_ROLES);
        if (claim instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
