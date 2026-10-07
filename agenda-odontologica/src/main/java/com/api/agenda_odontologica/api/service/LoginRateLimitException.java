package com.api.agenda_odontologica.api.service;

public class LoginRateLimitException extends RateLimitException {
    public LoginRateLimitException(long retryAfterSeconds) {
        super("Se superó el límite de intentos de inicio de sesión. Intentá nuevamente más tarde.",
                retryAfterSeconds);
    }
}
