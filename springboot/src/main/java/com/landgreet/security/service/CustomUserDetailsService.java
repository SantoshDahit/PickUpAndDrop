package com.landgreet.security.service;

import com.landgreet.repository.user.UserJpaRepository;
import com.landgreet.security.dto.UserDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Soft-deleted users must fail auth with the same outcome as unknown users.
        return userJpaRepository.findByEmailAndDeletedAtIsNull(email)
                .map(UserDetail::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user " + email));
    }
}
