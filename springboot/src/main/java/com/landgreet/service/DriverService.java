package com.landgreet.service;

import com.landgreet.dto.DriverDto;
import com.landgreet.entity.Driver;
import com.landgreet.exception.ApiException;
import com.landgreet.exception.ErrorCode;
import com.landgreet.repository.driver.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;

    @Transactional(readOnly = true)
    public Driver getById(String id) {
        return driverRepository.findById(id)
                .filter(driver -> !driver.isDeleted())
                .orElseThrow(() -> new ApiException(ErrorCode.DRIVER_IS_NOT_FOUND));
    }

    @Transactional
    public Driver save(Driver driver) {
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public Page<Driver> search(DriverDto.SearchRequest searchRequest, Pageable pageable) {
        return driverRepository.search(searchRequest, pageable);
    }
}
