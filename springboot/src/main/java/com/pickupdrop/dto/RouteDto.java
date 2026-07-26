package com.pickupdrop.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RouteDto {

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
    public static class TierResponse {
        private int groupSize;
        private int pricePerPerson;

        public TierResponse(int groupSize, int pricePerPerson) {
            this.groupSize = groupSize;
            this.pricePerPerson = pricePerPerson;
        }
    }
}
