package com.landgreet.support;

import com.landgreet.entity.User;
import com.landgreet.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
@RequiredArgsConstructor
public class TestAuthHelper {

    private final JwtTokenProvider jwtTokenProvider;

    public String bearerFor(User user) {
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
    }
}
