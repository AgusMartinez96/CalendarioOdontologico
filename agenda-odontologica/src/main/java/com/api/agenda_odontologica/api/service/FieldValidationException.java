package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.ApiError;

import java.util.List;

/** Validación de negocio con errores por campo; responde 400 con el JSON uniforme. */
public class FieldValidationException extends RuntimeException {
    private final transient List<ApiError.FieldViolation> violations;

    public FieldValidationException(List<ApiError.FieldViolation> violations) {
        super("La solicitud contiene datos inválidos.");
        this.violations = List.copyOf(violations);
    }

    public List<ApiError.FieldViolation> getViolations() {
        return violations;
    }
}
