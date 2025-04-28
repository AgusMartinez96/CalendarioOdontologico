package com.api.agenda_odontologica.dto;

import com.api.agenda_odontologica.entity.PostAppointment;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostAppointmentDTO {

    private Long id;
    private Boolean protesis;
    private LocalDateTime fecha;
    private String materiales;


    public PostAppointmentDTO() {
    }

    public PostAppointmentDTO(PostAppointment postAppointment) {
        this.id = postAppointment.getId();
        this.protesis = postAppointment.getProtesis();
        this.fecha = postAppointment.getFecha();
        this.materiales = postAppointment.getMateriales();
    }
}
