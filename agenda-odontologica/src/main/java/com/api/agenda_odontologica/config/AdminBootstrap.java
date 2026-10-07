package com.api.agenda_odontologica.config;

import com.api.agenda_odontologica.api.entity.UserAccount;
import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea o actualiza el usuario admin con ADMIN_USERNAME / ADMIN_PASSWORD. Los datos
 * previos a la multiusuario ya quedan asignados al admin en la migración V4.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String username,
            @Value("${app.admin.password}") String password) {
        if (password.isBlank()) {
            throw new IllegalStateException("La variable ADMIN_PASSWORD debe configurarse.");
        }
        if (username.isBlank()) {
            throw new IllegalStateException("La variable ADMIN_USERNAME no puede estar vacía.");
        }
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.username = username.trim();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserAccount admin = users.findFirstByRoleOrderByIdAsc(UserAccount.ROLE_ADMIN).orElse(null);
        UserAccount sameName = users.findByUsernameNormalized(UserAccount.normalize(username)).orElse(null);
        if (sameName != null && (admin == null || !sameName.getId().equals(admin.getId()))) {
            throw new IllegalStateException(
                    "ADMIN_USERNAME coincide con una cuenta de usuario existente que no es el admin.");
        }
        if (admin == null) {
            users.save(new UserAccount(username, passwordEncoder.encode(password), UserAccount.ROLE_ADMIN));
            log.info("Usuario admin creado.");
            return;
        }
        if (!admin.getUsername().equals(username)) {
            admin.setUsername(username);
        }
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(password));
        }
        users.save(admin);
    }
}
