package com.pickupdrop.repository.pricetier;

import com.pickupdrop.entity.PriceTier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PriceTierRepositoryImpl implements PriceTierRepository {

    private final PriceTierJpaRepository priceTierJpaRepository;

    @Override
    public List<PriceTier> findAllOrdered() {
        return priceTierJpaRepository.findAllByOrderByGroupSizeAsc();
    }

    @Override
    public List<PriceTier> findAllByRouteIdOrdered(String routeId) {
        return priceTierJpaRepository.findAllByRouteIdOrderByGroupSizeAsc(routeId);
    }

    @Override
    public List<PriceTier> saveAll(List<PriceTier> tiers) {
        return priceTierJpaRepository.saveAll(tiers);
    }

    @Override
    public void deleteAllByRouteId(String routeId) {
        priceTierJpaRepository.deleteAllByRouteId(routeId);
    }
}
