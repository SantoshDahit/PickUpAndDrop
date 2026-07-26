package com.pickupdrop.repository.pricetier;

import com.pickupdrop.entity.PriceTier;
import java.util.List;

public interface PriceTierRepository {

    List<PriceTier> findAllOrdered();

    List<PriceTier> findAllByRouteIdOrdered(String routeId);

    List<PriceTier> saveAll(List<PriceTier> tiers);

    void deleteAllByRouteId(String routeId);
}
