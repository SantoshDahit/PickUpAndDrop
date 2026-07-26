package com.pickupdrop.repository.travelgroup;

import com.pickupdrop.entity.TravelGroup;
import java.util.List;
import java.util.Optional;

public interface TravelGroupRepository {

    Optional<TravelGroup> findById(String id);

    List<TravelGroup> findOpenByRouteId(String routeId);

    List<TravelGroup> findAllByDriverId(String driverId);

    List<TravelGroup> findOpenPublicRides();

    TravelGroup save(TravelGroup travelGroup);
}
