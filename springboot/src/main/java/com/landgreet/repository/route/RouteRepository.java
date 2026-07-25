package com.landgreet.repository.route;

import com.landgreet.entity.Route;
import java.util.List;
import java.util.Optional;

public interface RouteRepository {

    Optional<Route> findById(String id);

    List<Route> findAllActive();

    Route save(Route route);
}
