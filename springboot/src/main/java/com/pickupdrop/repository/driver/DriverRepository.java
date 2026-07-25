package com.pickupdrop.repository.driver;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.entity.Driver;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DriverRepository {

    Optional<Driver> findById(String id);

    Driver save(Driver driver);

    Page<Driver> search(DriverDto.SearchRequest searchRequest, Pageable pageable);
}
