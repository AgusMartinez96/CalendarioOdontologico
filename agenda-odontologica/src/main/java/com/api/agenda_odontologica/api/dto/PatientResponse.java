package com.api.agenda_odontologica.api.dto;

import com.api.agenda_odontologica.api.entity.PatientRecord;

public record PatientResponse(
        Long id,
        String nombre,
        String apellido,
        String dni,
        String telefono,
        String email) {

    public PatientResponse(PatientRecord patient) {
        this(patient.getId(), patient.getNombre(), patient.getApellido(), patient.getDni(),
                patient.getTelefono(), patient.getEmail());
    }
}
