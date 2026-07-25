package com.pickupdrop.service;

import com.pickupdrop.entity.Route;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.repository.route.RouteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;

    @Transactional(readOnly = true)
    public Route getActiveById(String id) {
        return routeRepository.findById(id)
                .filter(Route::isActive)
                .orElseThrow(() -> new ApiException(ErrorCode.ROUTE_IS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Route> getAllActive() {
        return routeRepository.findAllActive();
    }

    @Transactional
    public Route save(Route route) {
        return routeRepository.save(route);
    }
}
