package com.landgreet.repository.route;

import com.landgreet.entity.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteJpaRepository extends JpaRepository<Route, String> {

    List<Route> findAllByActiveTrueOrderByCreatedAtAsc();
}
