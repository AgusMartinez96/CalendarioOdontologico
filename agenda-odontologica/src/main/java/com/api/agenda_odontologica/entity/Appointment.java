package com.api.agenda_odontologica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime fecha;
    private Boolean asistencia;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    //Constructor sin argumentos
    public Appointment() {
    }

    //Constructor con argumentos

    public Appointment(Long id, LocalDateTime fecha, Boolean asistencia, Patient patient) {
        this.id = id;
        this.fecha = fecha;
        this.asistencia = asistencia;
        this.patient = patient;
    }

    //Getters y Setters

}


