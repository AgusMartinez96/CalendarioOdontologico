package com.api.agenda_odontologica.dto;

import com.api.agenda_odontologica.entity.Appointment;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppointmentDTO {
    private Long id;
    private Long patientId;
    private LocalDateTime fecha;
    private Boolean asistencia;
    private PostAppointmentDTO postAppointment;

    public AppointmentDTO() {}

    public AppointmentDTO(Appointment appointment) {
        this.id = appointment.getId();
//        this.patientId = appointment.getPatient().getId();
        this.patientId = appointment.getPatient() != null ? appointment.getPatient().getId() : null;
        this.fecha = appointment.getFecha();
        this.asistencia = appointment.getAsistencia();
        this.postAppointment = appointment.getPostAppointment() != null ? new PostAppointmentDTO(appointment.getPostAppointment()) : null;
    }
}