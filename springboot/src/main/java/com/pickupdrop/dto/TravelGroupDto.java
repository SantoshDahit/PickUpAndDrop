package com.pickupdrop.dto;

import com.pickupdrop.enums.GroupStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TravelGroupDto {

    public record AdminPostRequest(
            @jakarta.validation.constraints.NotBlank String routeId,
            @jakarta.validation.constraints.NotNull LocalDate targetDate
    ) {
    }

    /** Public browse card — deliberately free of any personal data. */
    @Getter
    @NoArgsConstructor
    public static class OpenRideResponse {
        private String id;
        private RouteDto.Response route;
        private LocalDate targetDate;
        private int memberCount;
        private int seatsLeft;
        private LocalDate earliestDate;   // null while empty
        private LocalDate latestDate;

        public OpenRideResponse(String id, RouteDto.Response route, LocalDate targetDate,
                                int memberCount, int seatsLeft,
                                LocalDate earliestDate, LocalDate latestDate) {
            this.id = id;
            this.route = route;
            this.targetDate = targetDate;
            this.memberCount = memberCount;
            this.seatsLeft = seatsLeft;
            this.earliestDate = earliestDate;
            this.latestDate = latestDate;
        }
    }

    /** Suggestions for one booking: its landing week + joinable groups. No personal data. */
    @Getter
    @NoArgsConstructor
    public static class SuggestionsResponse {
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private List<SuggestionResponse> groups;

        public SuggestionsResponse(LocalDate weekStart, LocalDate weekEnd, List<SuggestionResponse> groups) {
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.groups = groups;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class SuggestionResponse {
        private String id;
        private int memberCount;
        private int seatsLeft;
        private LocalDate earliestDate;   // null while empty
        private LocalDate latestDate;
        private boolean official;         // admin-published ride
        private LocalDate targetDate;     // official rides only

        public SuggestionResponse(String id, int memberCount, int seatsLeft, LocalDate earliestDate,
                                  LocalDate latestDate, boolean official, LocalDate targetDate) {
            this.id = id;
            this.memberCount = memberCount;
            this.seatsLeft = seatsLeft;
            this.earliestDate = earliestDate;
            this.latestDate = latestDate;
            this.official = official;
            this.targetDate = targetDate;
        }
    }

    /**
     * Group page view. Members see each other's first name, party size,
     * preferred date and intro — never email, phone, contact or notes.
     */
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private RouteDto.Response route;
        private GroupStatus status;
        private List<MemberResponse> members;
        private LocalDate agreedDate;   // set when every member's date is identical
        private DriverDto.PublicResponse driver;   // null until the admin assigns one
        private LocalDate weekStart;    // the group's landing-week boundary
        private LocalDate weekEnd;

        public Response(String id, RouteDto.Response route, GroupStatus status,
                        List<MemberResponse> members, LocalDate agreedDate,
                        DriverDto.PublicResponse driver, LocalDate weekStart, LocalDate weekEnd) {
            this.id = id;
            this.route = route;
            this.status = status;
            this.members = members;
            this.agreedDate = agreedDate;
            this.driver = driver;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
        }
    }

    // ===== Admin chat moderation (plan 012) =====

    public record AdminAddMemberRequest(
            @jakarta.validation.constraints.NotBlank String bookingId
    ) {
    }

    /** One row of the admin chat index. Most-recently-active first. */
    @Getter
    @NoArgsConstructor
    public static class ChatSummaryResponse {
        private String id;
        private RouteDto.Response route;
        private GroupStatus status;
        private boolean official;          // admin-published ride
        private LocalDate targetDate;      // official rides only
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private int memberCount;
        private int seatsLeft;
        private int messageCount;
        private String lastMessagePreview;  // null when nobody has written yet
        private String lastMessageAuthor;
        private boolean lastMessageStaff;
        private LocalDateTime lastMessageAt;
        private boolean driverAssigned;

        public ChatSummaryResponse(String id, RouteDto.Response route, GroupStatus status,
                                   boolean official, LocalDate targetDate,
                                   LocalDate weekStart, LocalDate weekEnd,
                                   int memberCount, int seatsLeft, int messageCount,
                                   String lastMessagePreview, String lastMessageAuthor,
                                   boolean lastMessageStaff, LocalDateTime lastMessageAt,
                                   boolean driverAssigned) {
            this.id = id;
            this.route = route;
            this.status = status;
            this.official = official;
            this.targetDate = targetDate;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.memberCount = memberCount;
            this.seatsLeft = seatsLeft;
            this.messageCount = messageCount;
            this.lastMessagePreview = lastMessagePreview;
            this.lastMessageAuthor = lastMessageAuthor;
            this.lastMessageStaff = lastMessageStaff;
            this.lastMessageAt = lastMessageAt;
            this.driverAssigned = driverAssigned;
        }
    }

    /**
     * Admin group view: full identities, unlike the members' privacy-limited
     * {@link Response}. Never returned by a customer route (plan 012 §4.3).
     */
    @Getter
    @NoArgsConstructor
    public static class AdminDetailResponse {
        private String id;
        private RouteDto.Response route;
        private GroupStatus status;
        private boolean official;
        private LocalDate targetDate;
        private LocalDate weekStart;
        private LocalDate weekEnd;
        private int seatsLeft;
        private LocalDate agreedDate;
        private DriverDto.PublicResponse driver;
        private List<AdminMemberResponse> members;

        public AdminDetailResponse(String id, RouteDto.Response route, GroupStatus status,
                                   boolean official, LocalDate targetDate,
                                   LocalDate weekStart, LocalDate weekEnd, int seatsLeft,
                                   LocalDate agreedDate, DriverDto.PublicResponse driver,
                                   List<AdminMemberResponse> members) {
            this.id = id;
            this.route = route;
            this.status = status;
            this.official = official;
            this.targetDate = targetDate;
            this.weekStart = weekStart;
            this.weekEnd = weekEnd;
            this.seatsLeft = seatsLeft;
            this.agreedDate = agreedDate;
            this.driver = driver;
            this.members = members;
        }
    }

    /** A member as the operator needs to see them — contactable. */
    @Getter
    @NoArgsConstructor
    public static class AdminMemberResponse {
        private String bookingId;
        private String userId;
        private String name;
        private String email;
        private String phone;
        private String contact;
        private int partySize;
        private LocalDate travelDate;
        private String flightNo;
        private String intro;
        private String notes;

        public AdminMemberResponse(String bookingId, String userId, String name, String email,
                                   String phone, String contact, int partySize,
                                   LocalDate travelDate, String flightNo, String intro,
                                   String notes) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.contact = contact;
            this.partySize = partySize;
            this.travelDate = travelDate;
            this.flightNo = flightNo;
            this.intro = intro;
            this.notes = notes;
        }
    }

    /** A booking the admin could add to this group — already rule-checked. */
    @Getter
    @NoArgsConstructor
    public static class CandidateResponse {
        private String bookingId;
        private String name;
        private String email;
        private int partySize;
        private LocalDate travelDate;
        private String flightNo;
        private String currentGroupId;   // non-null = adding moves them

        public CandidateResponse(String bookingId, String name, String email, int partySize,
                                 LocalDate travelDate, String flightNo, String currentGroupId) {
            this.bookingId = bookingId;
            this.name = name;
            this.email = email;
            this.partySize = partySize;
            this.travelDate = travelDate;
            this.flightNo = flightNo;
            this.currentGroupId = currentGroupId;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class MemberResponse {
        private String firstName;
        private int partySize;
        private LocalDate travelDate;
        private String intro;
        private boolean me;

        public MemberResponse(String firstName, int partySize, LocalDate travelDate,
                              String intro, boolean me) {
            this.firstName = firstName;
            this.partySize = partySize;
            this.travelDate = travelDate;
            this.intro = intro;
            this.me = me;
        }
    }
}
