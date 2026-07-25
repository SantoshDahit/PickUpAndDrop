package com.landgreet.service;

import com.landgreet.entity.User;
import com.landgreet.exception.ApiException;
import com.landgreet.exception.ErrorCode;
import com.landgreet.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getById(String id) {
        return userRepository.findById(id)
                .filter(user -> !user.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_IS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User getActiveByEmail(String email) {
        return userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_IS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean existsActiveByEmail(String email) {
        return userRepository.existsActiveByEmail(email);
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }
}
