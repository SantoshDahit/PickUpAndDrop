package com.landgreet.repository.travelgroup;

import com.landgreet.entity.TravelGroup;
import com.landgreet.enums.GroupStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelGroupJpaRepository extends JpaRepository<TravelGroup, String> {

    List<TravelGroup> findAllByRouteIdAndStatusOrderByCreatedAtAsc(String routeId, GroupStatus status);
}
