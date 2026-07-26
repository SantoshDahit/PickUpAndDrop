package com.pickupdrop.controller;

import com.pickupdrop.dto.RouteDto;
import com.pickupdrop.entity.PriceTier;
import com.pickupdrop.mapper.RouteMapper;
import com.pickupdrop.service.PriceTierService;
import com.pickupdrop.service.RouteService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/routes")
@RequiredArgsConstructor
public class RouteController {

    // Read-only listing: services direct per the architecture doc's exception.
    private final RouteService routeService;
    private final PriceTierService priceTierService;
    private final RouteMapper routeMapper;

    /** Active routes with fare tiers (public — feeds the fare calculator). */
    @GetMapping
    public List<RouteDto.Response> getActiveRoutes() {
        Map<String, List<PriceTier>> tiersByRoute = priceTierService.getAllOrdered().stream()
                .collect(Collectors.groupingBy(tier -> tier.getRoute().getId()));
        return routeService.getAllActive().stream()
                .map(route -> {
                    RouteDto.Response response = routeMapper.toResponse(route);
                    response.setTiers(tiersByRoute.getOrDefault(route.getId(), List.of()).stream()
                            .map(t -> new RouteDto.TierResponse(t.getGroupSize(), t.getPricePerPerson()))
                            .toList());
                    return response;
                })
                .toList();
    }
}
