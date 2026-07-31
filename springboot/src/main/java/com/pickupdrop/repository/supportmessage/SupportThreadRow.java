package com.pickupdrop.repository.supportmessage;

import java.time.LocalDateTime;

/**
 * One row of the operator's support inbox, aggregated in a single query so the
 * inbox does not issue a query per thread.
 */
public record SupportThreadRow(
        String userId,
        String name,
        String email,
        String phone,
        long messageCount,
        long unreadFromTraveller,
        LocalDateTime lastMessageAt
) {
}
