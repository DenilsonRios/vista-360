package com.test_icesi.vista_360.academicrecord.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.test_icesi.vista_360.academicrecord.service.StudentNotFoundException;
import com.test_icesi.vista_360.academicrecord.service.TermNotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({StudentNotFoundException.class, TermNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso no encontrado", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acceso denegado", ex.getMessage());
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    public ProblemDetail handleValidation(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parámetros inválidos",
                "La solicitud contiene parámetros que no cumplen las restricciones esperadas");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        return body;
    }
}
