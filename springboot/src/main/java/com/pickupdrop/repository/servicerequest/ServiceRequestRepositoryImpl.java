package com.pickupdrop.repository.servicerequest;

import com.pickupdrop.entity.ServiceRequest;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ServiceRequestRepositoryImpl implements ServiceRequestRepository {

    private final ServiceRequestJpaRepository serviceRequestJpaRepository;

    @Override
    public Optional<ServiceRequest> findById(String id) {
        return serviceRequestJpaRepository.findById(id)
                .filter(request -> !request.isDeleted());
    }

    @Override
    public List<ServiceRequest> findAllByUserId(String userId) {
        return serviceRequestJpaRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<ServiceRequest> findQueue(ServiceType type, ServiceRequestStatus status) {
        return serviceRequestJpaRepository.findQueue(type, status);
    }

    @Override
    public ServiceRequest save(ServiceRequest serviceRequest) {
        return serviceRequestJpaRepository.save(serviceRequest);
    }
}
