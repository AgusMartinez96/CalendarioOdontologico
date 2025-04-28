package com.api.agenda_odontologica.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
public class PostAppointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Boolean protesis;
    private LocalDateTime fecha;
    private String materiales;

    @OneToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    //Constructor sin argumentos
    public PostAppointment() {
    }

    // Constructor con argumentos


    public PostAppointment(Long id, boolean protesis, LocalDateTime fecha, String materiales, Appointment appointment) {
        this.id = id;
        this.protesis = protesis;
        this.fecha = fecha;
        this.materiales = materiales;
        this.appointment = appointment;
    }
}

