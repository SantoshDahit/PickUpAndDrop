package com.pickupdrop.repository.route;

import com.pickupdrop.entity.Route;
import java.util.List;
import java.util.Optional;

public interface RouteRepository {

    Optional<Route> findById(String id);

    List<Route> findAllActive();

    Route save(Route route);
}
