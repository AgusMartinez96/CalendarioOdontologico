package com.api.agenda_odontologica.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Nunca debe imprimirse la contraseña: toString la oculta. */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
