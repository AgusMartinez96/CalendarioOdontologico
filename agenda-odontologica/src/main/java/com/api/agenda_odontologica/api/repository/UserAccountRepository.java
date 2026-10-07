package com.api.agenda_odontologica.api.repository;

import com.api.agenda_odontologica.api.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsernameNormalized(String usernameNormalized);

    boolean existsByUsernameNormalized(String usernameNormalized);

    Optional<UserAccount> findFirstByRoleOrderByIdAsc(String role);
}
