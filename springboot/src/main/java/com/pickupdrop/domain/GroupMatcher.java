package com.pickupdrop.domain;

import com.pickupdrop.entity.Booking;
import com.pickupdrop.entity.TravelGroup;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Matching policy (plan 002):
 * a booking may join a group when the group's preferred-date span stays
 * within MATCH_WINDOW_DAYS after joining (prevents chain drift) and the
 * seats fit. The window applies at matching time only — members may move
 * their dates freely afterwards; converging is what the chat is for.
 */
@Component
public class GroupMatcher {

    public static final int MATCH_WINDOW_DAYS = 7;

    public boolean qualifies(List<Booking> activeMembers, LocalDate travelDate, int partySize) {
        if (activeMembers.isEmpty()) {
            return false; // never match into an empty group
        }
        int seats = activeMembers.stream().mapToInt(Booking::getPartySize).sum();
        if (seats + partySize > TravelGroup.MAX_SEATS) {
            return false;
        }
        LocalDate min = travelDate;
        LocalDate max = travelDate;
        for (Booking member : activeMembers) {
            LocalDate d = member.getTravelDate();
            if (d.isBefore(min)) min = d;
            if (d.isAfter(max)) max = d;
        }
        return max.toEpochDay() - min.toEpochDay() <= MATCH_WINDOW_DAYS;
    }

    public int seatsOf(List<Booking> activeMembers) {
        return activeMembers.stream().mapToInt(Booking::getPartySize).sum();
    }
}
