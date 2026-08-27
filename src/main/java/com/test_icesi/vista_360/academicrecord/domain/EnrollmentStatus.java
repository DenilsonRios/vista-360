package com.test_icesi.vista_360.academicrecord.domain;

/** Estado de una matrícula de un estudiante en una oferta de materia. */
public enum EnrollmentStatus {
    /** Inscrita y en curso. */
    ENROLLED,
    /** Cancelada por el estudiante o la institución. */
    WITHDRAWN,
    /** Finalizada (con nota definitiva). */
    COMPLETED
}
