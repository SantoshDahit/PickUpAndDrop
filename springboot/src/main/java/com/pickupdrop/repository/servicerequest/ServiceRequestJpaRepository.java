package com.pickupdrop.repository.servicerequest;

import com.pickupdrop.entity.ServiceRequest;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ServiceRequestJpaRepository extends JpaRepository<ServiceRequest, String> {

    List<ServiceRequest> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(String userId);

    /**
     * Operator queue: open requests first (they are the work), then the settled
     * ones, each newest first. Traveller fetched — the queue shows who asked.
     *
     * <p>Requests from soft-deleted accounts are excluded: they cannot be
     * fulfilled (nobody to hand a SIM to, and the email has been renamed to
     * {@code deleted:...}), so they are history, not work. The row stays in the
     * table. Account deletion also cancels them going forward — this guard
     * covers rows orphaned before that existed.
     */
    @Query("""
           select r
             from ServiceRequest r
             join fetch r.user u
            where r.deletedAt is null
              and u.deletedAt is null
              and (:type is null or r.type = :type)
              and (:status is null or r.status = :status)
            order by case when r.status in (com.pickupdrop.enums.ServiceRequestStatus.REQUESTED,
                                            com.pickupdrop.enums.ServiceRequestStatus.CONFIRMED)
                          then 0 else 1 end,
                     r.createdAt desc
           """)
    List<ServiceRequest> findQueue(ServiceType type, ServiceRequestStatus status);
}
