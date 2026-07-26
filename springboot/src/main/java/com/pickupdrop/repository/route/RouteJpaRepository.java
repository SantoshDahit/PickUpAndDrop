package com.pickupdrop.repository.route;

import com.pickupdrop.entity.Route;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteJpaRepository extends JpaRepository<Route, String> {

    List<Route> findAllByActiveTrueOrderByCreatedAtAsc();

    List<Route> findAllByOrderByCreatedAtAsc();
}
