package com.landgreet.config;

import com.landgreet.user.User;
import com.landgreet.user.UserRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Flyway seeds structure; this runner seeds the bootstrap credential —
 * passwords don't belong in checked-in SQL.
 */
@Component
public class SeedAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedAdminRunner.class);
    private static final String DEFAULT_PASSWORD = "admin123";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public SeedAdminRunner(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin-email}") String adminEmail,
            @Value("${app.seed.admin-password}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (DEFAULT_PASSWORD.equals(adminPassword)) {
            log.warn("Admin seed password is the built-in default — set SEED_ADMIN_PASSWORD before going to production.");
        }
        if (users.count() > 0) {
            return;
        }
        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneOffset.UTC).format(Instant.now());
        users.save(new User("Admin", adminEmail, passwordEncoder.encode(adminPassword), null, true, now));
        log.info("Seeded admin account {}", adminEmail);
    }
}
