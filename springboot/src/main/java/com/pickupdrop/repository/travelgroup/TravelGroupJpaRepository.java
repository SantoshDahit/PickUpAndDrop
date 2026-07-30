package com.pickupdrop.repository.travelgroup;

import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.enums.GroupStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TravelGroupJpaRepository extends JpaRepository<TravelGroup, String> {

    List<TravelGroup> findAllByRouteIdAndStatusOrderByCreatedAtAsc(String routeId, GroupStatus status);

    List<TravelGroup> findAllByRouteIdAndWeekBucketAndStatusOrderByCreatedAtAsc(
            String routeId, String weekBucket, GroupStatus status);

    List<TravelGroup> findAllByDriverId(String driverId);

    List<TravelGroup> findAllByPublicRideTrueAndStatusOrderByTargetDateAsc(GroupStatus status);

    /** Admin chat index (plan 012): every group, newest first, route fetched. */
    @Query("select g from TravelGroup g join fetch g.route order by g.createdAt desc")
    List<TravelGroup> findAllForAdminIndex();

    boolean existsByRouteId(String routeId);
}
