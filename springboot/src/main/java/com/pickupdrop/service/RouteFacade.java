package com.pickupdrop.service;

import com.pickupdrop.dto.RouteDto;
import com.pickupdrop.entity.PriceTier;
import com.pickupdrop.entity.Route;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.mapper.RouteMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RouteFacade {

    private final RouteService routeService;
    private final PriceTierService priceTierService;
    private final BookingService bookingService;
    private final TravelGroupService travelGroupService;
    private final RouteMapper routeMapper;

    @Transactional
    public RouteDto.AdminResponse create(RouteDto.PostRequest request) {
        List<RouteDto.TierRequest> tiers = validatedTiers(request.tiers());
        Route route = routeService.save(new Route(request.fromLocation().trim(), request.toLocation().trim()));
        priceTierService.saveAll(toEntities(route, tiers));
        return toAdminResponse(route);
    }

    @Transactional(readOnly = true)
    public List<RouteDto.AdminResponse> getAll() {
        Map<String, List<PriceTier>> tiersByRoute = priceTierService.getAllOrdered().stream()
                .collect(Collectors.groupingBy(tier -> tier.getRoute().getId()));
        return routeService.getAllOrdered().stream()
                .map(route -> {
                    RouteDto.AdminResponse response = routeMapper.toAdminResponse(route);
                    response.setTiers(toTierResponses(tiersByRoute.getOrDefault(route.getId(), List.of())));
                    return response;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteDto.AdminResponse getById(String routeId) {
        return toAdminResponse(routeService.getById(routeId));
    }

    /** Partial update: null = keep; tiers present = full replace of the fare ladder. */
    @Transactional
    public RouteDto.AdminResponse update(String routeId, RouteDto.PatchRequest request) {
        Route route = routeService.getById(routeId);
        route.update(request.fromLocation(), request.toLocation(), request.active());
        if (request.tiers() != null) {
            priceTierService.replaceForRoute(route, toEntities(route, validatedTiers(request.tiers())));
        }
        return toAdminResponse(route);
    }

    /** Hard delete; refused while any booking or group references the route. */
    @Transactional
    public void delete(String routeId) {
        Route route = routeService.getById(routeId);
        if (bookingService.existsByRouteId(routeId) || travelGroupService.existsByRouteId(routeId)) {
            throw new ApiException(ErrorCode.ROUTE_IN_USE);
        }
        priceTierService.deleteAllForRoute(routeId);
        routeService.delete(route);
    }

    private RouteDto.AdminResponse toAdminResponse(Route route) {
        RouteDto.AdminResponse response = routeMapper.toAdminResponse(route);
        response.setTiers(toTierResponses(priceTierService.getByRouteIdOrdered(route.getId())));
        return response;
    }

    private static List<RouteDto.TierRequest> validatedTiers(List<RouteDto.TierRequest> tiers) {
        List<RouteDto.TierRequest> safe = tiers == null ? List.of() : tiers;
        long distinctSizes = safe.stream().map(RouteDto.TierRequest::groupSize).distinct().count();
        if (distinctSizes != safe.size()) {
            throw new ApiException(ErrorCode.ROUTE_TIERS_INVALID);
        }
        return safe;
    }

    private static List<PriceTier> toEntities(Route route, List<RouteDto.TierRequest> tiers) {
        return tiers.stream()
                .map(t -> new PriceTier(route, t.groupSize(), t.pricePerPerson()))
                .toList();
    }

    private static List<RouteDto.TierResponse> toTierResponses(List<PriceTier> tiers) {
        return tiers.stream()
                .map(t -> new RouteDto.TierResponse(t.getGroupSize(), t.getPricePerPerson()))
                .toList();
    }
}
