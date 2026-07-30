package com.pickupdrop.service;

import com.pickupdrop.entity.ServiceRequest;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import com.pickupdrop.repository.servicerequest.ServiceRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;

    @Transactional(readOnly = true)
    public ServiceRequest getById(String id) {
        return serviceRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_REQUEST_IS_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<ServiceRequest> getAllByUserId(String userId) {
        return serviceRequestRepository.findAllByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequest> getQueue(ServiceType type, ServiceRequestStatus status) {
        return serviceRequestRepository.findQueue(type, status);
    }

    @Transactional
    public ServiceRequest save(ServiceRequest serviceRequest) {
        return serviceRequestRepository.save(serviceRequest);
    }
}
