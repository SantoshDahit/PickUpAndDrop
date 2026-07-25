package com.landgreet.user;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * getUsername() deliberately returns the user id, not the email: the
 * principal name is stored in sessions (and their PRINCIPAL_NAME index) at
 * login time, and an id never goes stale when the user later changes their
 * email. Login still happens by email — see AppUserDetailsService.
 */
public class AppUserDetails implements UserDetails {

    private final long id;
    private final String email;
    private final String passwordHash;
    private final boolean admin;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.admin = user.isAdmin();
    }

    public long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }
}
