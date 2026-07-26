package com.pickupdrop.repository.travelgroup;

import com.pickupdrop.entity.TravelGroup;
import com.pickupdrop.enums.GroupStatus;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TravelGroupRepositoryImpl implements TravelGroupRepository {

    private final TravelGroupJpaRepository travelGroupJpaRepository;

    @Override
    public Optional<TravelGroup> findById(String id) {
        return travelGroupJpaRepository.findById(id);
    }

    @Override
    public List<TravelGroup> findOpenByRouteId(String routeId) {
        return travelGroupJpaRepository.findAllByRouteIdAndStatusOrderByCreatedAtAsc(routeId, GroupStatus.OPEN);
    }

    @Override
    public List<TravelGroup> findOpenByRouteIdAndWeekBucket(String routeId, String weekBucket) {
        return travelGroupJpaRepository.findAllByRouteIdAndWeekBucketAndStatusOrderByCreatedAtAsc(
                routeId, weekBucket, GroupStatus.OPEN);
    }

    @Override
    public List<TravelGroup> findAllByDriverId(String driverId) {
        return travelGroupJpaRepository.findAllByDriverId(driverId);
    }

    @Override
    public List<TravelGroup> findOpenPublicRides() {
        return travelGroupJpaRepository.findAllByPublicRideTrueAndStatusOrderByTargetDateAsc(GroupStatus.OPEN);
    }

    @Override
    public boolean existsByRouteId(String routeId) {
        return travelGroupJpaRepository.existsByRouteId(routeId);
    }

    @Override
    public TravelGroup save(TravelGroup travelGroup) {
        return travelGroupJpaRepository.save(travelGroup);
    }
}
