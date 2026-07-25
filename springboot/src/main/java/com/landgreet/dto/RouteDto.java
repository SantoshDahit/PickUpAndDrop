package com.landgreet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class RouteDto {

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String id;
        private String fromLocation;
        private String toLocation;
    }
}
