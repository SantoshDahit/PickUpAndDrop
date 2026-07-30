package com.pickupdrop.enums;

/**
 * What a {@link com.pickupdrop.entity.ServiceRequest} is asking for.
 *
 * <p>Only {@link #SIM_CARD} is requestable today; the type exists so the next
 * facility reuses this table, its endpoints and the admin queue rather than
 * adding a near-identical schema (plan 013 §4.1).
 */
public enum ServiceType {
    SIM_CARD
}
