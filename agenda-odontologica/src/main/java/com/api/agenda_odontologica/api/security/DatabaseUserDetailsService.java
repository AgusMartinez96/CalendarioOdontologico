package com.api.agenda_odontologica.api.security;

import com.api.agenda_odontologica.api.entity.UserAccount;
import com.api.agenda_odontologica.api.repository.UserAccountRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public DatabaseUserDetailsService(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        return users.findByUsernameNormalized(UserAccount.normalize(username))
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario inexistente"));
    }
}
