package com.api.agenda_odontologica.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** Ventana deslizante de una hora por IP, en memoria, con limpieza de entradas viejas. */
@Component
public class RegistrationRateLimiter {
    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> attemptsByIp = new HashMap<>();
    private final Clock clock;
    private final int maxPerWindow;

    public RegistrationRateLimiter(
            Clock clock, @Value("${app.registration.max-per-ip-per-hour:5}") int maxPerWindow) {
        if (maxPerWindow < 1) {
            throw new IllegalArgumentException("REGISTER_MAX_PER_IP_PER_HOUR debe ser mayor que cero.");
        }
        this.clock = clock;
        this.maxPerWindow = maxPerWindow;
    }

    /** Registra un intento y devuelve 0 si está permitido, o los segundos de espera si no. */
    public synchronized long acquire(String clientIp) {
        Instant now = clock.instant();
        cleanExpired(now);
        Deque<Instant> attempts = attemptsByIp.computeIfAbsent(clientIp, key -> new ArrayDeque<>());
        if (attempts.size() >= maxPerWindow) {
            long millis = Duration.between(now, attempts.peekFirst().plus(WINDOW)).toMillis();
            return Math.max(1, (millis + 999) / 1000);
        }
        attempts.addLast(now);
        return 0;
    }

    private void cleanExpired(Instant now) {
        Instant limit = now.minus(WINDOW);
        attemptsByIp.entrySet().removeIf(entry -> {
            Deque<Instant> attempts = entry.getValue();
            while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(limit)) {
                attempts.pollFirst();
            }
            return attempts.isEmpty();
        });
    }
}
