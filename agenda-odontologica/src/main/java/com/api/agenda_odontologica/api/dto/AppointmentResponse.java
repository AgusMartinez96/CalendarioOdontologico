package com.api.agenda_odontologica.api.dto;

import com.api.agenda_odontologica.api.entity.AppointmentRecord;
import com.api.agenda_odontologica.api.entity.AppointmentStatus;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Instant startAt,
        Instant endAt,
        Long patientId,
        String patientName,
        String motivo,
        String notas,
        AppointmentStatus estado) {

    public AppointmentResponse(AppointmentRecord appointment) {
        this(appointment.getId(), appointment.getStartAt(), appointment.getEndAt(),
                appointment.getPatient().getId(),
                appointment.getPatient().getNombre() + " " + appointment.getPatient().getApellido(),
                appointment.getMotivo(), appointment.getNotas(), appointment.getEstado());
    }
}
