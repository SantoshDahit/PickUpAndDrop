package com.pickupdrop.repository.pricetier;

import com.pickupdrop.entity.PriceTier;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceTierJpaRepository extends JpaRepository<PriceTier, String> {

    List<PriceTier> findAllByOrderByGroupSizeAsc();

    List<PriceTier> findAllByRouteIdOrderByGroupSizeAsc(String routeId);

    // Bulk JPQL so the delete hits the DB before replacement inserts flush
    // ((route_id, group_size) carries a unique key). Flush first so pending
    // changes land; clear after so stale managed tiers can't be re-flushed.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from PriceTier t where t.route.id = :routeId")
    void deleteAllByRouteId(@Param("routeId") String routeId);
}
