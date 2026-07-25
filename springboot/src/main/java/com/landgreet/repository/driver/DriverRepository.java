package com.landgreet.repository.driver;

import com.landgreet.dto.DriverDto;
import com.landgreet.entity.Driver;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DriverRepository {

    Optional<Driver> findById(String id);

    Driver save(Driver driver);

    Page<Driver> search(DriverDto.SearchRequest searchRequest, Pageable pageable);
}
