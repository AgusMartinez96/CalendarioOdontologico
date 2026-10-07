package com.api.agenda_odontologica.api.dto;

import jakarta.validation.constraints.NotNull;

/** Nunca debe imprimirse la contraseña: toString la oculta. */
public record RegisterRequest(@NotNull String username, @NotNull String password) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username + ", password=***]";
    }
}
