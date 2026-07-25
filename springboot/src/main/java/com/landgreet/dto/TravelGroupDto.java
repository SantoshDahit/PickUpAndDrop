package com.landgreet.dto;

import com.landgreet.enums.GroupStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TravelGroupDto {

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
