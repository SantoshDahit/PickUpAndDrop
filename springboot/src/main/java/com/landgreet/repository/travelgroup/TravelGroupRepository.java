package com.landgreet.repository.travelgroup;

import com.landgreet.entity.TravelGroup;
import java.util.List;
import java.util.Optional;

public interface TravelGroupRepository {

    Optional<TravelGroup> findById(String id);

    List<TravelGroup> findOpenByRouteId(String routeId);

    TravelGroup save(TravelGroup travelGroup);
}
