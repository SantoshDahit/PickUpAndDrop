package com.pickupdrop.enums;

/**
 * Workflow of a service request (plan 013 §4.2):
 * {@code REQUESTED → CONFIRMED → DELIVERED}, with {@code CANCELLED} reachable
 * from either open state. {@code DELIVERED} and {@code CANCELLED} are terminal.
 */
public enum ServiceRequestStatus {

    REQUESTED,
    CONFIRMED,
    DELIVERED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    /** Legal next states, so a transition is validated rather than assigned. */
    public boolean canMoveTo(ServiceRequestStatus next) {
        return switch (this) {
            case REQUESTED -> next == CONFIRMED || next == DELIVERED || next == CANCELLED;
            case CONFIRMED -> next == DELIVERED || next == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
