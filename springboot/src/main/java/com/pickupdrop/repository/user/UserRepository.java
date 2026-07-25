package com.pickupdrop.repository.user;

import com.pickupdrop.entity.User;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(String id);

    Optional<User> findActiveByEmail(String email);

    boolean existsActiveByEmail(String email);

    User save(User user);

    long count();
}
