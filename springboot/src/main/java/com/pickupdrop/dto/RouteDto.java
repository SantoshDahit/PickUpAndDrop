package com.pickupdrop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RouteDto {

    public record TierRequest(
            @Min(1) @Max(10) int groupSize,
            @Min(1) int pricePerPerson
    ) {
    }

    public record PostRequest(
            @NotBlank @Size(max = 200) String fromLocation,
            @NotBlank @Size(max = 200) String toLocation,
            @Valid List<TierRequest> tiers
    ) {
    }

    /** Partial update: null = keep; tiers present = full replace of the fare ladder. */
    public record PatchRequest(
            @Size(max = 200) String fromLocation,
            @Size(max = 200) String toLocation,
            Boolean active,
            @Valid List<TierRequest> tiers
    ) {
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String fromLocation;
        private String toLocation;
        private List<TierResponse> tiers;   // per-person fare by group size

        public void setTiers(List<TierResponse> tiers) {
            this.tiers = tiers;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class AdminResponse {
        private String id;
        private String fromLocation;
        private String toLocation;
        private boolean active;
        private List<TierResponse> tiers;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public void setTiers(List<TierResponse> tiers) {
            this.tiers = tiers;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class TierResponse {
        private int groupSize;
        private int pricePerPerson;

        public TierResponse(int groupSize, int pricePerPerson) {
            this.groupSize = groupSize;
            this.pricePerPerson = pricePerPerson;
        }
    }
}
