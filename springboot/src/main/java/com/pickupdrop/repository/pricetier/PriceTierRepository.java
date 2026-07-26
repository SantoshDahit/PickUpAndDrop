package com.pickupdrop.repository.pricetier;

import com.pickupdrop.entity.PriceTier;
import java.util.List;

public interface PriceTierRepository {

    List<PriceTier> findAllOrdered();
}
