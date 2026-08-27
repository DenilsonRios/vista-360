package com.test_icesi.vista_360.academicrecord.service;

/** El periodo académico solicitado no existe (o no hay un periodo marcado como vigente). */
public class TermNotFoundException extends RuntimeException {

    public TermNotFoundException(String termCode) {
        super("No existe el periodo académico '" + termCode + "'");
    }
}
