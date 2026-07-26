package com.pickupdrop.dto;

import com.pickupdrop.enums.BookingStatus;
import com.pickupdrop.enums.MatchPref;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class BookingDto {

    public record PostRequest(
            @NotBlank String routeId,
            String groupId,            // optional: join a specific published ride
            @NotNull LocalDate travelDate,
            @Size(max = 20) String flightNo,
            @Min(1) @Max(6) int partySize,
            @NotNull MatchPref matchPref,
            @Size(max = 300) String intro,
            @Size(max = 100) String contact,
            @Size(max = 1000) String notes
    ) {
    }

    public record PatchRequest(
            @NotNull LocalDate travelDate
    ) {
    }

    /** null groupId = start a new group for the booking's landing week. */
    public record SelectGroupRequest(
            String groupId
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private RouteDto.Response route;
        private String groupId;              // null = riding individually
        private Boolean joinedExistingGroup; // set on creation only
        private LocalDate travelDate;
        private String flightNo;
        private int partySize;
        private MatchPref matchPref;
        private String intro;
        private String contact;
        private String notes;
        private BookingStatus status;
        private DriverDto.PublicResponse driver;   // effective: group's when grouped
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public void setDriver(DriverDto.PublicResponse driver) {
            this.driver = driver;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public void setJoinedExistingGroup(Boolean joinedExistingGroup) {
            this.joinedExistingGroup = joinedExistingGroup;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class SummaryResponse {
        private String id;
        private RouteDto.Response route;
        private String groupId;
        private LocalDate travelDate;
        private String flightNo;
        private int partySize;
        private MatchPref matchPref;
        private BookingStatus status;
        private DriverDto.PublicResponse driver;   // effective: group's when grouped
        private LocalDateTime createdAt;

        public void setDriver(DriverDto.PublicResponse driver) {
            this.driver = driver;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }

    /** Admin ops view — includes who booked and how to reach them. */
    @Getter
    @NoArgsConstructor
    public static class AdminDetailResponse {
        private String id;
        private RouteDto.Response route;
        private String groupId;
        private LocalDate travelDate;
        private String flightNo;
        private int partySize;
        private MatchPref matchPref;
        private String intro;
        private String contact;
        private String notes;
        private BookingStatus status;
        private DriverDto.PublicResponse driver;   // effective: group's when grouped
        private CustomerResponse customer;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public void setDriver(DriverDto.PublicResponse driver) {
            this.driver = driver;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public void setCustomer(CustomerResponse customer) {
            this.customer = customer;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class CustomerResponse {
        private String name;
        private String email;
        private String phone;

        public CustomerResponse(String name, String email, String phone) {
            this.name = name;
            this.email = email;
            this.phone = phone;
        }
    }

    public record SearchRequest(
            String routeId,
            List<BookingStatus> statusList,
            LocalDate minTravelDate,
            LocalDate maxTravelDate
    ) {
    }
}
