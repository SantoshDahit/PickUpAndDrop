package com.pickupdrop.dto;

import com.pickupdrop.enums.DriverStatus;
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
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
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
