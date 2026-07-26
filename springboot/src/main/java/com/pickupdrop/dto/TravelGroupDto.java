package com.pickupdrop.dto;

import com.pickupdrop.enums.GroupStatus;
import java.time.LocalDate;
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

        public Response(String id, RouteDto.Response route, GroupStatus status,
                        List<MemberResponse> members, LocalDate agreedDate,
                        DriverDto.PublicResponse driver) {
            this.id = id;
            this.route = route;
            this.status = status;
            this.members = members;
            this.agreedDate = agreedDate;
            this.driver = driver;
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
