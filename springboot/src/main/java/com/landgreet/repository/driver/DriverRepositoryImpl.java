package com.landgreet.repository.driver;

import com.landgreet.dto.DriverDto;
import com.landgreet.entity.Driver;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DriverRepositoryImpl implements DriverRepository {

    private final DriverJpaRepository driverJpaRepository;
    private final DriverQueryRepository driverQueryRepository;

    @Override
    public Optional<Driver> findById(String id) {
        return driverJpaRepository.findById(id);
    }

    @Override
    public Driver save(Driver driver) {
        return driverJpaRepository.save(driver);
    }

    @Override
    public Page<Driver> search(DriverDto.SearchRequest searchRequest, Pageable pageable) {
        return driverQueryRepository.search(searchRequest, pageable);
    }
}
