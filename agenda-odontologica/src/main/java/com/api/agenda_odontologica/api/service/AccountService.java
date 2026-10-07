package com.api.agenda_odontologica.api.service;

import com.api.agenda_odontologica.api.dto.ApiError;
import com.api.agenda_odontologica.api.entity.UserAccount;
import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import com.api.agenda_odontologica.api.security.AppUserDetails;
import com.api.agenda_odontologica.api.security.CommonPasswords;
import com.api.agenda_odontologica.api.security.RegistrationRateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Alta de cuentas y reglas de acceso. Nunca registra ni devuelve contraseñas ni hashes. */
@Service
public class AccountService {
    static final String USERNAME_TAKEN = "Ese nombre de usuario ya está en uso";
    static final String CAPACITY_REACHED = "Registro cerrado por cupo";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");
    private static final int USERNAME_MIN = 3;
    private static final int USERNAME_MAX = 30;
    private static final int PASSWORD_MIN = 10;
    private static final int PASSWORD_MAX_BYTES = 72;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationRateLimiter rateLimiter;
    private final Clock clock;
    private final TransactionTemplate transaction;
    private final boolean registrationEnabled;
    private final long maxUsers;
    private final Object registrationLock = new Object();

    public AccountService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            RegistrationRateLimiter rateLimiter,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${app.registration.enabled:true}") boolean registrationEnabled,
            @Value("${app.registration.max-users:50}") long maxUsers) {
        if (maxUsers < 1) {
            throw new IllegalArgumentException("MAX_USERS debe ser mayor que cero.");
        }
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
        this.registrationEnabled = registrationEnabled;
        this.maxUsers = maxUsers;
    }

    @Transactional(readOnly = true)
    public boolean registrationOpen() {
        return registrationEnabled && users.count() < maxUsers;
    }

    public AppUserDetails register(String username, String password, String clientIp) {
        if (!registrationEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "El registro de cuentas nuevas está deshabilitado por el administrador.");
        }
        validate(username, password);
        long retryAfter = rateLimiter.acquire(clientIp);
        if (retryAfter > 0) {
            throw new RateLimitException(
                    "Hiciste demasiados intentos de registro. Esperá antes de volver a intentar.", retryAfter);
        }
        if (users.count() >= maxUsers) {
            throw new ApiException(HttpStatus.CONFLICT, CAPACITY_REACHED);
        }
        if (users.existsByUsernameNormalized(UserAccount.normalize(username))) {
            throw new ApiException(HttpStatus.CONFLICT, USERNAME_TAKEN);
        }

        String hash = passwordEncoder.encode(password);
        UserAccount saved;
        synchronized (registrationLock) {
            try {
                saved = transaction.execute(status -> {
                    if (users.count() >= maxUsers) {
                        throw new ApiException(HttpStatus.CONFLICT, CAPACITY_REACHED);
                    }
                    UserAccount account = new UserAccount(username, hash, UserAccount.ROLE_USER);
                    account.setLastLoginAt(clock.instant());
                    return users.saveAndFlush(account);
                });
            } catch (DataIntegrityViolationException exception) {
                throw new ApiException(HttpStatus.CONFLICT, USERNAME_TAKEN);
            }
        }
        AppUserDetails principal = new AppUserDetails(saved);
        principal.eraseCredentials();
        return principal;
    }

    @Transactional
    public void recordLogin(Long userId) {
        users.findById(userId).ifPresent(user -> user.setLastLoginAt(clock.instant()));
    }

    private static void validate(String username, String password) {
        List<ApiError.FieldViolation> violations = new ArrayList<>();
        String name = username == null ? "" : username;
        String secret = password == null ? "" : password;

        if (name.length() < USERNAME_MIN || name.length() > USERNAME_MAX) {
            violations.add(new ApiError.FieldViolation("username",
                    "El usuario debe tener entre 3 y 30 caracteres."));
        } else if (!USERNAME_PATTERN.matcher(name).matches()) {
            violations.add(new ApiError.FieldViolation("username",
                    "El usuario solo puede tener letras, números, punto, guion y guion bajo, "
                            + "y debe empezar con una letra o un número."));
        }

        if (secret.codePointCount(0, secret.length()) < PASSWORD_MIN) {
            violations.add(new ApiError.FieldViolation("password",
                    "La contraseña debe tener al menos 10 caracteres."));
        } else if (secret.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            violations.add(new ApiError.FieldViolation("password",
                    "La contraseña no puede superar los 72 bytes en UTF-8 "
                            + "(las tildes y los emojis ocupan más de un byte)."));
        } else if (secret.equalsIgnoreCase(name)) {
            violations.add(new ApiError.FieldViolation("password",
                    "La contraseña no puede ser igual al nombre de usuario."));
        } else if (CommonPasswords.contains(secret)) {
            violations.add(new ApiError.FieldViolation("password",
                    "Esa contraseña es demasiado común. Elegí otra."));
        }

        if (!violations.isEmpty()) {
            throw new FieldValidationException(violations);
        }
    }
}
