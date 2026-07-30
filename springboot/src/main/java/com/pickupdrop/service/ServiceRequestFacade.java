package com.pickupdrop.service;

import com.pickupdrop.dto.ServiceRequestDto;
import com.pickupdrop.entity.ServiceRequest;
import com.pickupdrop.entity.User;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import com.pickupdrop.exception.ApiException;
import com.pickupdrop.exception.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ServiceRequestFacade {

    private final ServiceRequestService serviceRequestService;
    private final UserService userService;

    @Transactional
    public ServiceRequestDto.Response create(String userId, ServiceRequestDto.PostRequest request) {
        validateArrivalDate(request.arrivalDate());
        User user = userService.getById(userId);
        ServiceRequest created = serviceRequestService.save(new ServiceRequest(
                user,
                request.type(),
                request.arrivalDate(),
                blankToNull(request.airport()),
                blankToNull(request.detail()),
                blankToNull(request.deliverTo()),
                blankToNull(request.contact()),
                blankToNull(request.notes())));
        return toResponse(created);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestDto.Response> getMine(String userId) {
        return serviceRequestService.getAllByUserId(userId).stream()
                .map(ServiceRequestFacade::toResponse)
                .toList();
    }

    /** Traveller cancels their own request while it is still open. */
    @Transactional
    public void cancel(String userId, String requestId) {
        ServiceRequest request = serviceRequestService.getById(requestId);
        if (!request.getUser().getId().equals(userId)) {
            // 404, not 403 — do not confirm someone else's request exists.
            throw new ApiException(ErrorCode.SERVICE_REQUEST_IS_NOT_FOUND);
        }
        if (!request.updateStatus(ServiceRequestStatus.CANCELLED)) {
            throw new ApiException(ErrorCode.SERVICE_REQUEST_STATUS_INVALID);
        }
        serviceRequestService.save(request);
    }

    // ===== Admin =====

    @Transactional(readOnly = true)
    public List<ServiceRequestDto.AdminResponse> getQueue(ServiceType type, ServiceRequestStatus status) {
        return serviceRequestService.getQueue(type, status).stream()
                .map(ServiceRequestFacade::toAdminResponse)
                .toList();
    }

    /** Move the status and/or leave an internal note. Illegal moves are refused. */
    @Transactional
    public ServiceRequestDto.AdminResponse update(String requestId,
                                                  ServiceRequestDto.AdminPatchRequest request) {
        ServiceRequest serviceRequest = serviceRequestService.getById(requestId);
        if (request.status() != null && !serviceRequest.updateStatus(request.status())) {
            throw new ApiException(ErrorCode.SERVICE_REQUEST_STATUS_INVALID);
        }
        if (request.adminNote() != null) {
            serviceRequest.updateAdminNote(blankToNull(request.adminNote()));
        }
        return toAdminResponse(serviceRequestService.save(serviceRequest));
    }

    private static void validateArrivalDate(LocalDate arrivalDate) {
        if (arrivalDate == null) {
            return; // optional: a traveller may not have their date yet
        }
        LocalDate today = LocalDate.now();
        if (arrivalDate.isBefore(today) || arrivalDate.isAfter(today.plusDays(365))) {
            throw new ApiException(ErrorCode.SERVICE_REQUEST_DATE_IS_INVALID);
        }
    }

    private static ServiceRequestDto.Response toResponse(ServiceRequest request) {
        return new ServiceRequestDto.Response(
                request.getId(), request.getType(), request.getStatus(), request.getArrivalDate(),
                request.getAirport(), request.getDetail(), request.getDeliverTo(),
                request.getContact(), request.getNotes(), request.getCreatedAt());
    }

    private static ServiceRequestDto.AdminResponse toAdminResponse(ServiceRequest request) {
        User user = request.getUser();
        return new ServiceRequestDto.AdminResponse(
                request.getId(), request.getType(), request.getStatus(), request.getArrivalDate(),
                request.getAirport(), request.getDetail(), request.getDeliverTo(),
                request.getContact(), request.getNotes(), request.getAdminNote(),
                user.getName(), user.getEmail(), user.getPhone(),
                request.getCreatedAt(), request.getUpdatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
