package com.pickupdrop.config;

import com.pickupdrop.entity.User;
import com.pickupdrop.enums.Role;
import com.pickupdrop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Flyway seeds structure; credentials are seeded at runtime, never in SQL. */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeedAdminRunner implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "admin123";

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (DEFAULT_PASSWORD.equals(adminPassword)) {
            log.warn("Admin seed password is the built-in default — set SEED_ADMIN_PASSWORD before production.");
        }
        if (userService.count() > 0) {
            return;
        }
        userService.save(new User(adminEmail, passwordEncoder.encode(adminPassword), "Admin", null, Role.ADMIN));
        log.info("Seeded admin account {}", adminEmail);
    }
}
