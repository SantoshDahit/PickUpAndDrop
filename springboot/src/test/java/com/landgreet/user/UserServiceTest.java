package com.landgreet.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.landgreet.storage.ObjectStorage;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;

class UserServiceTest {

    private UserRepository repository;
    private ObjectStorage storage;
    private FindByIndexNameSessionRepository<?> sessions;
    private UserService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(UserRepository.class);
        storage = mock(ObjectStorage.class);
        sessions = mock(FindByIndexNameSessionRepository.class);
        when(sessions.findByPrincipalName(anyString())).thenReturn(Map.of());
        when(repository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UserService(repository, new BCryptPasswordEncoder(4), storage,
                new AvatarImageProcessor(), (FindByIndexNameSessionRepository) sessions);
    }

    private static User userWithId(long id, String name, String email, String passwordHash) throws Exception {
        var user = new User(name, email, passwordHash, null, false, "2026-07-26 00:00:00");
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(UserService.normalizeEmail("  Minji@Example.COM ")).isEqualTo("minji@example.com");
        assertThat(UserService.normalizeEmail(null)).isEmpty();
    }

    @Test
    void registerStoresNormalizedEmailAndHashedPassword() {
        when(repository.existsByEmailAndDeletedAtIsNull("minji@example.com")).thenReturn(false);

        User saved = service.register(" Minji Kim ", " Minji@Example.com ", null, "secret1");

        assertThat(saved.getEmail()).isEqualTo("minji@example.com");
        assertThat(saved.getName()).isEqualTo("Minji Kim");
        assertThat(saved.getPasswordHash()).isNotEqualTo("secret1").startsWith("$2");
    }

    @Test
    void registerMapsUniqueConstraintRaceToDuplicateEmail() {
        when(repository.existsByEmailAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(repository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("UNIQUE"));

        assertThatThrownBy(() -> service.register("A", "a@b.com", null, "secret1"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registerRejectsTakenEmail() {
        when(repository.existsByEmailAndDeletedAtIsNull("taken@b.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("A", "taken@b.com", null, "secret1"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void softDeleteWithWrongPasswordChangesNothing() throws Exception {
        var encoder = new BCryptPasswordEncoder(4);
        var user = userWithId(7, "Minji", "minji@example.com", encoder.encode("right"));
        when(repository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(service.softDeleteSelf(7, "wrong")).isFalse();
        assertThat(user.getDeletedAt()).isNull();
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void softDeleteFreesEmailAndDeletesAvatarAndSessions() throws Exception {
        var encoder = new BCryptPasswordEncoder(4);
        var user = userWithId(7, "Minji", "minji@example.com", encoder.encode("right"));
        user.setAvatarKey("avatars/abc.jpg");
        when(repository.findById(7L)).thenReturn(Optional.of(user));

        assertThat(service.softDeleteSelf(7, "right")).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("deleted:7:minji@example.com");
        assertThat(user.getAvatarKey()).isNull();
        verify(storage).delete("avatars/abc.jpg");
        verify(sessions).findByPrincipalName("7");
    }

    @Test
    void reactivateRestoresOriginalEmail() throws Exception {
        var user = userWithId(7, "Minji", "deleted:7:minji@example.com", "hash");
        user.setDeletedAt("2026-07-26 00:00:00");
        when(repository.findById(7L)).thenReturn(Optional.of(user));

        service.setActive(1, 7, true);

        assertThat(user.getEmail()).isEqualTo("minji@example.com");
        assertThat(user.getDeletedAt()).isNull();
    }

    @Test
    void adminCannotTargetThemselves() {
        assertThatThrownBy(() -> service.setActive(1, 1, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.setAdmin(1, 1, false)).isInstanceOf(IllegalArgumentException.class);
    }
}
