package com.pickupdrop.enums;

/**
 * Lifecycle of an {@link com.pickupdrop.entity.EmailVerification}, same
 * vocabulary as {@code SmsStatus} in convention 17.
 *
 * <p>A two-step channel (SMS) walks {@code PENDING → VERIFIED → USED}: prove
 * you hold the number, then spend the proof. An emailed link is one step —
 * possession of the link *is* the proof — so the password-reset flow goes
 * {@code PENDING → USED} and never sets {@link #VERIFIED}. The state exists for
 * future flows that separate "clicked the link" from "completed the action".
 */
public enum EmailStatus {

    PENDING,
    VERIFIED,
    USED,
    EXPIRED
}
