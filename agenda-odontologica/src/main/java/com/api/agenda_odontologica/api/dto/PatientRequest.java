package com.api.agenda_odontologica.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Size(max = 30) String dni,
        @NotBlank @Size(max = 40) String telefono,
        @Email @Size(max = 254) String email) {
}
