package com.landgreet.user;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Deactivated accounts fail here with the same outcome as a wrong
        // password — no account-state oracle.
        return users.findByEmailAndDeletedAtIsNull(UserService.normalizeEmail(email))
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));
    }
}
