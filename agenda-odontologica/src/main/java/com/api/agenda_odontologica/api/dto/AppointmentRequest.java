package com.api.agenda_odontologica.api.dto;

import com.api.agenda_odontologica.api.entity.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AppointmentRequest(
        @NotNull @Future Instant startAt,
        @NotNull Long patientId,
        @NotBlank @Size(max = 200) String motivo,
        @Size(max = 5000) String notas,
        AppointmentStatus estado) {}
