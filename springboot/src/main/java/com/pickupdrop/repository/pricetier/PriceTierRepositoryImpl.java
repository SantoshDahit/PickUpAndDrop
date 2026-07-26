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
}
