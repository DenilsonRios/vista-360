package com.test_icesi.vista_360.academicrecord.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.test_icesi.vista_360.academicrecord.api.dto.AcademicRecordResponse;
import com.test_icesi.vista_360.academicrecord.security.AccessControlService;
import com.test_icesi.vista_360.academicrecord.service.AcademicRecordService;

import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
public class AcademicRecordController {
    private final AcademicRecordService academicRecordService;
    private final AccessControlService accessControlService;

    @GetMapping("/{studentCode}/academic-record")
    public AcademicRecordResponse getAcademicRecord(
            @PathVariable
            @Pattern(regexp = "[A-Za-z0-9-]{3,20}", message = "código de estudiante inválido")
            String studentCode,
            @RequestParam(name = "term", required = false)
            @Pattern(regexp = "\\d{4}-[12]", message = "el periodo debe tener el formato AAAA-1 o AAAA-2")
            String term,
            @AuthenticationPrincipal Jwt token) {
        accessControlService.checkCanViewStudent(studentCode, token);
        return academicRecordService.getAcademicRecord(studentCode, term);
    }
}
