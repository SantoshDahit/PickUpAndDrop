package com.pickupdrop.repository.route;

import com.pickupdrop.entity.Route;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RouteRepositoryImpl implements RouteRepository {

    private final RouteJpaRepository routeJpaRepository;

    @Override
    public Optional<Route> findById(String id) {
        return routeJpaRepository.findById(id);
    }

    @Override
    public List<Route> findAllActive() {
        return routeJpaRepository.findAllByActiveTrueOrderByCreatedAtAsc();
    }

    @Override
    public Route save(Route route) {
        return routeJpaRepository.save(route);
    }
}
