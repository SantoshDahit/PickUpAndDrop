package com.pickupdrop.dto;

import com.pickupdrop.enums.DriverStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class DriverDto {

    public record PostRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 30) String phone,
            @Size(max = 50) String licenseNo,
            boolean ownsVehicle,
            @Size(max = 100) String vehicle,
            @Size(max = 20) String plateNo,
            @Min(1) @Max(10) int seats
    ) {
    }

    public record PatchRequest(
            @Size(max = 100) String name,
            @Size(max = 30) String phone,
            @Size(max = 50) String licenseNo,
            Boolean ownsVehicle,
            @Size(max = 100) String vehicle,
            @Size(max = 20) String plateNo,
            @Min(1) @Max(10) Integer seats
    ) {
    }

    public record StatusPatchRequest(
            @NotNull DriverStatus status
    ) {
    }

    public record AssignRequest(
            @NotBlank String driverId
    ) {
    }

    public record AccountPostRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 6, max = 200) String password
    ) {
    }

    /** Only the facts a driver maintains themselves. */
    public record MePatchRequest(
            @Size(max = 30) String phone,
            @Size(max = 100) String vehicle,
            @Size(max = 20) String plateNo
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class RideResponse {
        private String rideType;          // GROUP | INDIVIDUAL
        private String rideId;            // groupId or bookingId
        private RouteDto.Response route;
        private java.time.LocalDate earliestDate;
        private java.time.LocalDate latestDate;   // equals earliestDate when agreed / individual
        private int totalSeats;
        private java.util.List<PassengerResponse> passengers;

        public RideResponse(String rideType, String rideId, RouteDto.Response route,
                            java.time.LocalDate earliestDate, java.time.LocalDate latestDate,
                            int totalSeats, java.util.List<PassengerResponse> passengers) {
            this.rideType = rideType;
            this.rideId = rideId;
            this.route = route;
            this.earliestDate = earliestDate;
            this.latestDate = latestDate;
            this.totalSeats = totalSeats;
            this.passengers = passengers;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class PassengerResponse {
        private String firstName;
        private int partySize;
        private java.time.LocalDate travelDate;
        private String flightNo;
        private String contact;   // drivers need to reach passengers at arrivals

        public PassengerResponse(String firstName, int partySize, java.time.LocalDate travelDate,
                                 String flightNo, String contact) {
            this.firstName = firstName;
            this.partySize = partySize;
            this.travelDate = travelDate;
            this.flightNo = flightNo;
            this.contact = contact;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String name;
        private String phone;
        private String licenseNo;
        private boolean ownsVehicle;
        private String vehicle;
        private String plateNo;
        private int seats;
        private DriverStatus status;
        private boolean hasAccount;   // login linked (005)
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public void setHasAccount(boolean hasAccount) {
            this.hasAccount = hasAccount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class SummaryResponse {
        private String id;
        private String name;
        private String phone;
        private String vehicle;
        private String plateNo;
        private int seats;
        private DriverStatus status;
    }

    /** Traveller-facing: strictly what you need to find your van at arrivals. */
    @Getter
    @NoArgsConstructor
    public static class PublicResponse {
        private String name;
        private String phone;
        private String vehicle;
        private String plateNo;
        private int seats;
    }

    public record SearchRequest(
            String name,
            List<DriverStatus> statusList,
            Integer minSeats
    ) {
    }
}
