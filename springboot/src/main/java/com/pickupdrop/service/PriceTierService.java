package com.pickupdrop.service;

import com.pickupdrop.entity.PriceTier;
import com.pickupdrop.entity.Route;
import com.pickupdrop.repository.pricetier.PriceTierRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceTierService {

    private final PriceTierRepository priceTierRepository;

    @Transactional(readOnly = true)
    public List<PriceTier> getAllOrdered() {
        return priceTierRepository.findAllOrdered();
    }

    @Transactional(readOnly = true)
    public List<PriceTier> getByRouteIdOrdered(String routeId) {
        return priceTierRepository.findAllByRouteIdOrdered(routeId);
    }

    @Transactional
    public List<PriceTier> saveAll(List<PriceTier> tiers) {
        return priceTierRepository.saveAll(tiers);
    }

    /** Full replace of a route's fare ladder. */
    @Transactional
    public List<PriceTier> replaceForRoute(Route route, List<PriceTier> tiers) {
        priceTierRepository.deleteAllByRouteId(route.getId());
        return priceTierRepository.saveAll(tiers);
    }

    @Transactional
    public void deleteAllForRoute(String routeId) {
        priceTierRepository.deleteAllByRouteId(routeId);
    }
}
