package com.test_icesi.vista_360.academicrecord.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.test_icesi.vista_360.academicrecord.api.dto.AcademicRecordResponse;
import com.test_icesi.vista_360.academicrecord.api.dto.EnrolledCourseResponse;
import com.test_icesi.vista_360.academicrecord.api.dto.StudentSummary;
import com.test_icesi.vista_360.academicrecord.api.dto.TermSummary;
import com.test_icesi.vista_360.academicrecord.security.AccessControlService;
import com.test_icesi.vista_360.academicrecord.service.AcademicRecordService;
import com.test_icesi.vista_360.config.SecurityConfig;

@WebMvcTest(AcademicRecordController.class)
@Import({SecurityConfig.class, ApiExceptionHandler.class})
class AcademicRecordControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AcademicRecordService academicRecordService;

    @MockitoBean
    AccessControlService accessControlService;

    @MockitoBean
    JwtDecoder jwtDecoder;

    private static final String PATH = "/api/v1/students/A00123456/academic-record";

    @Test
    void returnsAcademicRecordForAuthenticatedUser() throws Exception {
        when(academicRecordService.getAcademicRecord(eq("A00123456"), any()))
                .thenReturn(new AcademicRecordResponse(
                        new StudentSummary("A00123456", "Laura Gómez", "Ingeniería de Sistemas"),
                        new TermSummary("2025-2", "Segundo semestre 2025"),
                        List.of(new EnrolledCourseResponse("IS-101", "Intro", 3, "01",
                                "Ana Torres", "ENROLLED", new BigDecimal("4.30")))));

        mvc.perform(get(PATH).with(jwt().jwt(j -> j.claim("roles", List.of("STUDENT"))
                        .claim("student_id", "A00123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.code").value("A00123456"))
                .andExpect(jsonPath("$.enrolledCourses[0].courseCode").value("IS-101"))
                .andExpect(jsonPath("$.enrolledCourses[0].grade").value(4.30));
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsForbiddenWhenAccessControlDenies() throws Exception {
        doThrow(new AccessDeniedException("no autorizado"))
                .when(accessControlService).checkCanViewStudent(eq("A00123456"), any());

        mvc.perform(get(PATH).with(jwt().jwt(j -> j.claim("roles", List.of("STUDENT"))
                        .claim("student_id", "A00999999"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Acceso denegado"));
    }

    @Test
    void returnsBadRequestForMalformedTerm() throws Exception {
        mvc.perform(get(PATH).param("term", "invalido")
                        .with(jwt().jwt(j -> j.claim("roles", List.of("STUDENT"))
                                .claim("student_id", "A00123456"))))
                .andExpect(status().isBadRequest());
    }
}
