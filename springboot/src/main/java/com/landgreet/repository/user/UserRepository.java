package com.landgreet.repository.user;

import com.landgreet.entity.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String id);

    Optional<User> findActiveByEmail(String email);

    boolean existsActiveByEmail(String email);

    User save(User user);

    long count();
}
