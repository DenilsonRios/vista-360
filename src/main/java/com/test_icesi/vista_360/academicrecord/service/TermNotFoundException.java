package com.test_icesi.vista_360.academicrecord.service;

public class TermNotFoundException extends RuntimeException {
    public TermNotFoundException(String termCode) {
        super("No existe el periodo académico '" + termCode + "'");
    }
}
