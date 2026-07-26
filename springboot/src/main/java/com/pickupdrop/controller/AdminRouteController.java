package com.pickupdrop.controller;

import com.pickupdrop.dto.RouteDto;
import com.pickupdrop.service.RouteFacade;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/routes")
@RequiredArgsConstructor
public class AdminRouteController {

    private final RouteFacade routeFacade;

    /** Create a route with its fare ladder. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteDto.AdminResponse create(@RequestBody @Valid RouteDto.PostRequest request) {
        return routeFacade.create(request);
    }

    /** All routes (active and inactive) with tiers. */
    @GetMapping
    public List<RouteDto.AdminResponse> getAll() {
        return routeFacade.getAll();
    }

    /** Route detail. */
    @GetMapping("/{routeId}")
    public RouteDto.AdminResponse getById(@PathVariable String routeId) {
        return routeFacade.getById(routeId);
    }

    /** Partial update (null = keep); tiers present = full replace. */
    @PatchMapping("/{routeId}")
    public RouteDto.AdminResponse update(
            @PathVariable String routeId,
            @RequestBody @Valid RouteDto.PatchRequest request) {
        return routeFacade.update(routeId, request);
    }

    /** Hard delete; refused while bookings or rides reference the route. */
    @DeleteMapping("/{routeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String routeId) {
        routeFacade.delete(routeId);
    }
}
