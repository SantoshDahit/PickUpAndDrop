package com.landgreet.repository.driver;

import com.landgreet.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverJpaRepository extends JpaRepository<Driver, String> {
}
