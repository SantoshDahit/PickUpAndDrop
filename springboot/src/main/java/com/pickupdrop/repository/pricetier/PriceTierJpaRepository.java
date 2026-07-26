package com.pickupdrop.repository.pricetier;

import com.pickupdrop.entity.PriceTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTierJpaRepository extends JpaRepository<PriceTier, String> {

    List<PriceTier> findAllByOrderByGroupSizeAsc();
}
