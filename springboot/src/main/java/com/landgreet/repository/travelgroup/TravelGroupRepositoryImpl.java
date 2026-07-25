package com.landgreet.repository.travelgroup;

import com.landgreet.entity.TravelGroup;
import com.landgreet.enums.GroupStatus;
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
    public TravelGroup save(TravelGroup travelGroup) {
        return travelGroupJpaRepository.save(travelGroup);
    }
}
