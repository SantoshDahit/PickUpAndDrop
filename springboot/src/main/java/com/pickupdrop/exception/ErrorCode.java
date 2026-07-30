package com.pickupdrop.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * errorCode format: {DOMAIN}_{HTTP}_{SEQ}
 * Domains: CMN(common), USR(user), RTE(route), BKG(booking), GRP(group), MSG(message), DRV(driver)
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
    DRIVER_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            "Driver not found.",
            "DRV_NF_001"),
    DRIVER_PROFILE_NOT_LINKED(HttpStatus.NOT_FOUND,
            "No driver profile is linked to this account.",
            "DRV_NF_002"),

    // 400 Bad Request
    USER_DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST,
            "That email is already registered.",
            "USR_BR_001"),
    USER_PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST,
            "Current password is wrong.",
            "USR_BR_002"),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST,
            // One message for unknown / expired / already-used: the response
            // must not tell an attacker which reset links exist.
            "That reset link is no longer valid — request a new one.",
            "USR_BR_003"),
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
            "MSG_BR_001"),
    BOOKING_IS_GROUPED(HttpStatus.BAD_REQUEST,
            "This booking rides with a group — assign the driver to the group instead.",
            "BKG_BR_003"),
    DRIVER_IS_NOT_ASSIGNABLE(HttpStatus.BAD_REQUEST,
            "This driver is not available for assignment.",
            "DRV_BR_001"),
    DRIVER_SEATS_INSUFFICIENT(HttpStatus.BAD_REQUEST,
            "This driver's vehicle doesn't have enough seats for the ride.",
            "DRV_BR_002"),
    DRIVER_HAS_UPCOMING_RIDES(HttpStatus.BAD_REQUEST,
            "This driver still has upcoming rides — unassign them first.",
            "DRV_BR_003"),
    DRIVER_ACCOUNT_EXISTS(HttpStatus.BAD_REQUEST,
            "This driver already has a login.",
            "DRV_BR_004"),
    GROUP_NOT_JOINABLE(HttpStatus.BAD_REQUEST,
            "This ride can't be joined.",
            "GRP_BR_001"),
    GROUP_DATE_OUT_OF_WINDOW(HttpStatus.BAD_REQUEST,
            "Your landing day falls outside this group's landing week.",
            "GRP_BR_002"),
    GROUP_SEATS_FULL(HttpStatus.BAD_REQUEST,
            "This ride doesn't have enough free seats.",
            "GRP_BR_003"),
    SERVICE_REQUEST_IS_NOT_FOUND(HttpStatus.NOT_FOUND,
            // 404 for someone else's request too — never confirm it exists.
            "Service request not found.",
            "SVC_NF_001"),
    SERVICE_REQUEST_STATUS_INVALID(HttpStatus.BAD_REQUEST,
            "That request can no longer be changed.",
            "SVC_BR_001"),
    SERVICE_REQUEST_DATE_IS_INVALID(HttpStatus.BAD_REQUEST,
            "Pick an arrival day between today and a year from now.",
            "SVC_BR_002"),
    GROUP_HAS_MEMBERS(HttpStatus.BAD_REQUEST,
            "This ride still has members — handle them first.",
            "GRP_BR_004"),
    ROUTE_TIERS_INVALID(HttpStatus.BAD_REQUEST,
            "Each group size may appear only once in the fare ladder.",
            "RTE_BR_001"),
    ROUTE_IN_USE(HttpStatus.BAD_REQUEST,
            "This route has bookings or rides — deactivate it instead of deleting.",
            "RTE_BR_002");

    private final HttpStatus httpStatus;
    private final String message;
    private final String errorCode;
}
