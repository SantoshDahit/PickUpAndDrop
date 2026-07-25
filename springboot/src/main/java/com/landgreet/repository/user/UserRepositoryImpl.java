package com.landgreet.repository.user;

import com.landgreet.entity.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findById(String id) {
        return userJpaRepository.findById(id);
    }

    @Override
    public Optional<User> findActiveByEmail(String email) {
        return userJpaRepository.findByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public boolean existsActiveByEmail(String email) {
        return userJpaRepository.existsByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public User save(User user) {
        return userJpaRepository.save(user);
    }

    @Override
    public long count() {
        return userJpaRepository.count();
    }
}
