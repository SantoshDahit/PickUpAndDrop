package com.pickupdrop.repository.route;

import com.pickupdrop.entity.Route;
import java.util.List;
import java.util.Optional;

public interface RouteRepository {

    Optional<Route> findById(String id);

    List<Route> findAllActive();

    List<Route> findAllOrdered();

    Route save(Route route);

    void delete(Route route);
}
