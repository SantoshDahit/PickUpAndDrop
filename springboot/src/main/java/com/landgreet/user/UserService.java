package com.landgreet.user;

import com.landgreet.storage.ObjectStorage;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
public class UserService {

    private static final DateTimeFormatter SQLITE_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ObjectStorage storage;
    private final AvatarImageProcessor avatarProcessor;
    private final FindByIndexNameSessionRepository<? extends Session> sessions;

    public UserService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            ObjectStorage storage,
            AvatarImageProcessor avatarProcessor,
            FindByIndexNameSessionRepository<? extends Session> sessions) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.storage = storage;
        this.avatarProcessor = avatarProcessor;
        this.sessions = sessions;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    @Transactional(readOnly = true)
    public Optional<User> findActive(long id) {
        return users.findById(id).filter(u -> !u.isDeleted());
    }

    @Transactional(readOnly = true)
    public List<User> findAllForAdmin() {
        return users.findAllByOrderByCreatedAtDesc();
    }

    public User register(String name, String email, String phone, String rawPassword) {
        String normalized = normalizeEmail(email);
        if (users.existsByEmailAndDeletedAtIsNull(normalized)) {
            throw new DuplicateEmailException();
        }
        var user = new User(name.trim(), normalized, passwordEncoder.encode(rawPassword),
                phone == null ? null : phone.trim(), false, now());
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two signups raced past the existence check; UNIQUE(email) is the arbiter.
            throw new DuplicateEmailException();
        }
    }

    public User updateProfile(long userId, String name, String email, String phone) {
        var user = requireActive(userId);
        String normalized = normalizeEmail(email);
        if (!normalized.equals(user.getEmail()) && users.existsByEmailAndDeletedAtIsNull(normalized)) {
            throw new DuplicateEmailException();
        }
        user.setName(name.trim());
        user.setEmail(normalized);
        user.setPhone(phone == null ? null : phone.trim());
        user.setUpdatedAt(now());
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEmailException();
        }
    }

    /** @return false when the current password didn't match. */
    public boolean changePassword(long userId, String currentPassword, String newPassword) {
        var user = requireActive(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return false;
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now());
        users.save(user);
        return true;
    }

    public void updateAvatar(long userId, InputStream upload) {
        var user = requireActive(userId);
        byte[] jpeg = avatarProcessor.toSquareJpeg(upload);
        String newKey = "avatars/" + UUID.randomUUID() + ".jpg";
        String oldKey = user.getAvatarKey();

        // Write new → point row at it → delete old. A crash mid-sequence
        // leaves an orphan file, never a broken profile.
        storage.put(newKey, jpeg, "image/jpeg");
        user.setAvatarKey(newKey);
        user.setUpdatedAt(now());
        users.saveAndFlush(user);
        if (oldKey != null) {
            storage.delete(oldKey);
        }
    }

    public void removeAvatar(long userId) {
        var user = requireActive(userId);
        String oldKey = user.getAvatarKey();
        user.setAvatarKey(null);
        user.setUpdatedAt(now());
        users.saveAndFlush(user);
        if (oldKey != null) {
            storage.delete(oldKey);
        }
    }

    /** @return false when the password didn't match (account untouched). */
    public boolean softDeleteSelf(long userId, String password) {
        var user = requireActive(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return false;
        }
        deactivate(user);
        return true;
    }

    public void setActive(long actorId, long targetId, boolean active) {
        if (actorId == targetId) {
            throw new IllegalArgumentException("You can't deactivate your own account from the admin panel.");
        }
        var user = users.findById(targetId).orElseThrow();
        if (active) {
            // Restore the original address saved by deactivate(); see below.
            user.setEmail(user.getEmail().replaceFirst("^deleted:\\d+:", ""));
            user.setDeletedAt(null);
            user.setUpdatedAt(now());
            users.save(user);
        } else if (!user.isDeleted()) {
            deactivate(user);
        }
    }

    public void setAdmin(long actorId, long targetId, boolean admin) {
        if (actorId == targetId) {
            throw new IllegalArgumentException("You can't change your own admin role.");
        }
        var user = users.findById(targetId).orElseThrow();
        user.setAdmin(admin);
        user.setUpdatedAt(now());
        users.save(user);
    }

    /**
     * Soft delete: blocks login, frees the email slot for re-registration by
     * renaming it to deleted:{id}:{original} (keeps UNIQUE honest), keeps the
     * row for booking history, and kills every live session immediately.
     */
    private void deactivate(User user) {
        var avatarKey = user.getAvatarKey();
        user.setDeletedAt(now());
        user.setEmail("deleted:" + user.getId() + ":" + user.getEmail());
        user.setAvatarKey(null);
        user.setUpdatedAt(now());
        users.saveAndFlush(user);
        if (avatarKey != null) {
            storage.delete(avatarKey);
        }
        expireSessionsAfterCommit(user.getId());
    }

    /**
     * Session deletion must wait for the surrounding transaction to commit:
     * SQLite has a single writer, and Spring Session deletes on a second
     * connection — doing it inside our write transaction self-deadlocks.
     */
    private void expireSessionsAfterCommit(long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    expireSessions(userId);
                }
            });
        } else {
            expireSessions(userId);
        }
    }

    private void expireSessions(long userId) {
        // Sessions are indexed by principal name = user id (AppUserDetails.getUsername()).
        sessions.findByPrincipalName(String.valueOf(userId)).keySet().forEach(sessions::deleteById);
    }

    private User requireActive(long userId) {
        return findActive(userId).orElseThrow(
                () -> new IllegalStateException("No active user with id " + userId));
    }

    private static String now() {
        return SQLITE_DATETIME.format(Instant.now());
    }
}
