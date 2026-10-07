package com.api.agenda_odontologica.api.security;

import com.api.agenda_odontologica.api.entity.UserAccount;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Usuario autenticado; su id es la única fuente válida del dueño de los datos. */
public class AppUserDetails implements UserDetails, CredentialsContainer {
    private final Long id;
    private final String username;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;
    private String passwordHash;

    public AppUserDetails(UserAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.passwordHash = account.getPasswordHash();
        this.enabled = account.isEnabled();
        this.authorities = List.of(new SimpleGrantedAuthority(account.getRole()));
    }

    public Long getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    @Override
    public String toString() {
        return "AppUserDetails[id=" + id + ", username=" + username + "]";
    }
}
