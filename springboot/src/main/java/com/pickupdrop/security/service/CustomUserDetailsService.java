package com.pickupdrop.security.service;

import com.pickupdrop.repository.user.UserJpaRepository;
import com.pickupdrop.security.dto.UserDetail;
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
