package com.pickupdrop.service;

import com.pickupdrop.entity.PriceTier;
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
}
