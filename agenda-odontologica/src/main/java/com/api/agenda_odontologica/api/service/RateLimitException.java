package com.api.agenda_odontologica.api.service;

/** Se superó un límite de tasa; el cliente debe esperar retryAfterSeconds. */
public class RateLimitException extends RuntimeException {
    private final long retryAfterSeconds;

    public RateLimitException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
