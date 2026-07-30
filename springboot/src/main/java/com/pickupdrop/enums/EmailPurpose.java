package com.pickupdrop.enums;

import lombok.Getter;

/**
 * Why an {@link com.pickupdrop.entity.EmailVerification} was issued, and how
 * long it stays valid. Mirrors {@code SmsPurpose} in convention 17; an emailed
 * link gets a longer window than an SMS PIN because people read mail later.
 *
 * <p>Only {@link #PASSWORD_RESET} is wired today — the rest are the vocabulary
 * for flows that will reuse this table (see plan 011 §3 for what is out of scope).
 */
@Getter
public enum EmailPurpose {

    PASSWORD_RESET(60),
    VERIFY_ACCOUNT(60),
    JOIN(60),
    DELETE_ACCOUNT(30);

    private final int validMinutes;

    EmailPurpose(int validMinutes) {
        this.validMinutes = validMinutes;
    }
}
