package com.pickupdrop.support;

import com.pickupdrop.entity.User;
import com.pickupdrop.security.jwt.JwtTokenProvider;
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
