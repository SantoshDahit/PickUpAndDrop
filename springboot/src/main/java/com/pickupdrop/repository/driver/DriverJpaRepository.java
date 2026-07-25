package com.pickupdrop.repository.driver;

import com.pickupdrop.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverJpaRepository extends JpaRepository<Driver, String> {
}
