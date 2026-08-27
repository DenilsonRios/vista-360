package com.test_icesi.vista_360.academicrecord.service;

/** El estudiante solicitado no existe en la proyección académica. */
public class StudentNotFoundException extends RuntimeException {

    public StudentNotFoundException(String studentCode) {
        super("No existe un estudiante con código '" + studentCode + "'");
    }
}
