package com.api.agenda_odontologica.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppointmentRequest {
    private AppointmentDTO appointmentDTO;
    private PatientDTO patientDTO;
}