package com.pickupdrop.common;

public final class AuthConstants {

    public static final long ACCESS_TOKEN_EXPIRY_MILLIS = 3L * 60 * 60 * 1000;          // 3 hours
    public static final long REFRESH_TOKEN_EXPIRY_MILLIS = 90L * 24 * 60 * 60 * 1000;   // 90 days

    public static final String BEARER_PREFIX = "Bearer ";

    private AuthConstants() {
    }
}
