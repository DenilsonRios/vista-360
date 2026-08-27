package com.test_icesi.vista_360.academicrecord.service;

public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String studentCode) {
        super("No existe un estudiante con código '" + studentCode + "'");
    }
}
