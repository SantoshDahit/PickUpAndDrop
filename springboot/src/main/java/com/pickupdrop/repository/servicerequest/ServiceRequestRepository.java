package com.pickupdrop.repository.servicerequest;

import com.pickupdrop.entity.ServiceRequest;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository {

    Optional<ServiceRequest> findById(String id);

    List<ServiceRequest> findAllByUserId(String userId);

    /** Operator queue, optionally narrowed by type and/or status. */
    List<ServiceRequest> findQueue(ServiceType type, ServiceRequestStatus status);

    ServiceRequest save(ServiceRequest serviceRequest);
}
