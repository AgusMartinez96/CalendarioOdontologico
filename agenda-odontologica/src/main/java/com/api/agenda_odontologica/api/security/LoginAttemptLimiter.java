package com.api.agenda_odontologica.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class LoginAttemptLimiter {
    private final Map<Key, AttemptState> attempts = new HashMap<>();
    private final Clock clock;
    private final int maxAttempts;
    private final Duration window;
    private final Duration lockDuration;

    public LoginAttemptLimiter(
            Clock clock,
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.window-minutes:15}") long windowMinutes,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        if (maxAttempts < 1 || windowMinutes < 1 || lockMinutes < 1) {
            throw new IllegalArgumentException("Las propiedades del límite de login deben ser mayores que cero.");
        }
        this.clock = clock;
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofMinutes(windowMinutes);
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public synchronized long retryAfterSeconds(String clientIp, String username) {
        Instant now = clock.instant();
        cleanExpired(now);
        AttemptState state = attempts.get(key(clientIp, username));
        if (state == null || state.lockedUntil == null) {
            return 0;
        }
        return secondsUntil(state.lockedUntil, now);
    }

    public synchronized long recordFailure(String clientIp, String username) {
        Instant now = clock.instant();
        cleanExpired(now);
        Key key = key(clientIp, username);
        AttemptState state = attempts.get(key);
        if (state == null || !now.isBefore(state.windowStarted.plus(window))) {
            state = new AttemptState(now);
            attempts.put(key, state);
        }

        state.failures++;
        if (state.failures <= maxAttempts) {
            return 0;
        }

        state.lockedUntil = now.plus(lockDuration);
        return secondsUntil(state.lockedUntil, now);
    }

    public synchronized void recordSuccess(String clientIp, String username) {
        attempts.remove(key(clientIp, username));
    }

    private void cleanExpired(Instant now) {
        attempts.entrySet().removeIf(entry -> {
            AttemptState state = entry.getValue();
            Instant expiration = state.lockedUntil == null
                    ? state.windowStarted.plus(window)
                    : state.lockedUntil;
            return !now.isBefore(expiration);
        });
    }

    private static Key key(String clientIp, String username) {
        return new Key(clientIp, username.toLowerCase(Locale.ROOT));
    }

    private static long secondsUntil(Instant deadline, Instant now) {
        long milliseconds = Duration.between(now, deadline).toMillis();
        return Math.max(1, (milliseconds + 999) / 1000);
    }

    private record Key(String clientIp, String username) {
    }

    private static final class AttemptState {
        private final Instant windowStarted;
        private int failures;
        private Instant lockedUntil;

        private AttemptState(Instant windowStarted) {
            this.windowStarted = windowStarted;
        }
    }
}
