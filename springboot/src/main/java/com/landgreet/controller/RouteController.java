package com.landgreet.controller;

import com.landgreet.dto.RouteDto;
import com.landgreet.mapper.RouteMapper;
import com.landgreet.service.RouteService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    // Simple read-only listing: Service direct per the architecture doc's
    // exception (single service, trivial DTO mapping).
    private final RouteService routeService;
    private final RouteMapper routeMapper;

    /** Active routes (public — feeds the booking form and landing page). */
    @GetMapping
    public List<RouteDto.Response> getActiveRoutes() {
        return routeService.getAllActive().stream().map(routeMapper::toResponse).toList();
    }
}
