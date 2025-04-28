package com.api.agenda_odontologica.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String apellido;
    private String dni;
    private String obraSocial;
  //

    //Constructor sin argumentos
    public Patient() {
    }

    //Constructor con argumentos

    public Patient(Long id, String firstName, String apellido, String dni, String obraSocial) {
        this.id = id;
        this.nombre = firstName;
        this.apellido = apellido;
        this.dni = dni;
        this.obraSocial = obraSocial;
    }
}