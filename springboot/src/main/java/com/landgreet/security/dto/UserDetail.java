package com.landgreet.security.dto;

import com.landgreet.entity.User;
import com.landgreet.enums.Role;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserDetail implements UserDetails {

    private final String userId;
    private final String email;
    private final String password;
    private final Role role;

    public UserDetail(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.role = user.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
