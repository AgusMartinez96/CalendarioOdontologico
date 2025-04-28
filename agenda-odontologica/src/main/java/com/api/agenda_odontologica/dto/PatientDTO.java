package com.api.agenda_odontologica.dto;

import com.api.agenda_odontologica.entity.Patient;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PatientDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String obraSocial;

    public PatientDTO() {
    }

    public PatientDTO(Patient patient) {
        this.id = patient.getId();
        this.nombre = patient.getNombre();
        this.apellido = patient.getApellido();
        this.dni = patient.getDni();
        this.obraSocial = patient.getObraSocial();
    }
}