package com.landgreet.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * errorCode format: {DOMAIN}_{HTTP}_{SEQ}
 * Domains: CMN(common), USR(user), RTE(route), BKG(booking), GRP(group), MSG(message)
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong on our side. Please try again.",
            "CMN_ISE_001"),

    // 401 Unauthorized
    JWT_TOKEN_IS_INVALID(HttpStatus.UNAUTHORIZED,
            "Your session token is missing or invalid.",
            "CMN_UA_001"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED,
            "Email or password didn't match.",
            "USR_UA_001"),

    // 403 Forbidden
    AUTHENTICATION_INVALID(HttpStatus.FORBIDDEN,
            "You don't have access to this resource.",
            "CMN_FB_001"),

    // 404 Not Found
    USER_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "User not found.",
            "USR_NF_001"),
    ROUTE_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "Route not found.",
            "RTE_NF_001"),
    BOOKING_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "Booking not found.",
            "BKG_NF_001"),
    GROUP_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "Group not found.",
            "GRP_NF_001"),

    // 400 Bad Request
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,
            "That email is already registered.",
            "USR_BR_001"),
    USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST,
            "Current password is wrong.",
            "USR_BR_002"),
    BOOKING_DATE_IS_INVALID(HttpStatus.BAD_REQUEST,
            "Pick a landing day between today and a year from now.",
            "BKG_BR_001"),
    BOOKING_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST,
            "This booking is already cancelled.",
            "BKG_BR_002"),
    GROUP_MEMBERSHIP_REQUIRED(HttpStatus.NOT_FOUND,
            "Group not found.",
            "GRP_NF_002"), // non-members get 404, not 403 — do not confirm the group exists
    MESSAGE_BODY_IS_INVALID(HttpStatus.BAD_REQUEST,
            "Messages need 1-1000 characters.",
            "MSG_BR_001");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
