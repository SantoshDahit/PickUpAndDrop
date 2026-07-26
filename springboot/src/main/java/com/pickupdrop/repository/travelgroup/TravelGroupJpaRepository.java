package com.pickupdrop.repository.travelgroup;

import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.enums.GroupStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelGroupJpaRepository extends JpaRepository<TravelGroup, String> {

    List<TravelGroup> findAllByRouteIdAndStatusOrderByCreatedAtAsc(String routeId, GroupStatus status);

    List<TravelGroup> findAllByDriverId(String driverId);
}
