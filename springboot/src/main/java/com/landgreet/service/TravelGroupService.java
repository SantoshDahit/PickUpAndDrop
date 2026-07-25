package com.landgreet.service;

import com.landgreet.entity.TravelGroup;
import com.landgreet.exception.ApiException;
import com.landgreet.exception.ErrorCode;
import com.landgreet.repository.travelgroup.TravelGroupRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TravelGroupService {

    private final TravelGroupRepository travelGroupRepository;

    @Transactional(readOnly = true)
    public TravelGroup getById(String id) {
        return travelGroupRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.GROUP_IS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<TravelGroup> getOpenByRouteId(String routeId) {
        return travelGroupRepository.findOpenByRouteId(routeId);
    }

    @Transactional
    public TravelGroup save(TravelGroup travelGroup) {
        return travelGroupRepository.save(travelGroup);
    }
}
