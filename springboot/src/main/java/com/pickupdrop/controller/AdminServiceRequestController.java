package com.pickupdrop.controller;

import com.pickupdrop.dto.ServiceRequestDto;
import com.pickupdrop.enums.ServiceRequestStatus;
import com.pickupdrop.enums.ServiceType;
import com.pickupdrop.service.ServiceRequestFacade;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Service-request work queue for the operator. ADMIN via the route rule. */
@RestController
@RequestMapping("/v1/admin/service-requests")
@RequiredArgsConstructor
public class AdminServiceRequestController {

    private final ServiceRequestFacade serviceRequestFacade;

    /** Queue with traveller identity; open requests first. */
    @GetMapping
    public List<ServiceRequestDto.AdminResponse> queue(
            @RequestParam(required = false) ServiceType type,
            @RequestParam(required = false) ServiceRequestStatus status) {
        return serviceRequestFacade.getQueue(type, status);
    }

    /** Move the status and/or leave an internal note. */
    @PatchMapping("/{requestId}")
    public ServiceRequestDto.AdminResponse update(
            @PathVariable String requestId,
            @RequestBody @Valid ServiceRequestDto.AdminPatchRequest request) {
        return serviceRequestFacade.update(requestId, request);
    }
}
