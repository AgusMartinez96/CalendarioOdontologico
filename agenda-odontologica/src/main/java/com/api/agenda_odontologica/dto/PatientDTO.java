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

    // Constructor vacío
    public PatientDTO() {}

    // Constructor que recibe Patient
    public PatientDTO(Patient patient) {
        this.id = patient.getId();
        this.nombre = patient.getNombre();
        this.apellido = patient.getApellido();
        this.dni = patient.getDni();
        this.obraSocial = patient.getObraSocial();
    }

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getObraSocial() { return obraSocial; }
    public void setObraSocial(String obraSocial) { this.obraSocial = obraSocial; }
}