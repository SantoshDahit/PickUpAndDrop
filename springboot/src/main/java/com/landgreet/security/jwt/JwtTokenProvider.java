package com.landgreet.security.jwt;

import com.landgreet.common.AuthConstants;
import com.landgreet.security.service.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final CustomUserDetailsService userDetailsService;

    @Value("${jwt.token.secret}")
    private String tokenSecret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(tokenSecret.getBytes());
    }

    public String createAccessToken(String userId, String email) {
        return createToken(userId, email, AuthConstants.ACCESS_TOKEN_EXPIRY_MILLIS);
    }

    public String createRefreshToken(String userId, String email) {
        return createToken(userId, email, AuthConstants.REFRESH_TOKEN_EXPIRY_MILLIS);
    }

    private String createToken(String userId, String email, long validityMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + validityMillis))
                .claim("userId", userId)
                .signWith(getSigningKey())
                .compact();
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearer.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid JWT token", e);
        }
    }

    public String getUsernameByToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(getUsernameByToken(token));
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
