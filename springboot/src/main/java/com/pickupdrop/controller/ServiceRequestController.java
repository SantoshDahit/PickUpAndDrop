package com.pickupdrop.controller;

import com.pickupdrop.dto.ServiceRequestDto;
import com.pickupdrop.service.ServiceRequestFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Traveller-facing service requests (plan 013) — SIM cards today. */
@RestController
@RequestMapping("/v1/service-requests")
@RequiredArgsConstructor
public class ServiceRequestController {

    private final ServiceRequestFacade serviceRequestFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestDto.Response create(@RequestBody @Valid ServiceRequestDto.PostRequest request) {
        return serviceRequestFacade.create(
                AuthorizationUtil.getCurrentUser().getUserId(), request);
    }

    /** My requests, newest first. */
    @GetMapping("/me")
    public List<ServiceRequestDto.Response> getMine() {
        return serviceRequestFacade.getMine(AuthorizationUtil.getCurrentUser().getUserId());
    }

    /** Cancel my own request while it is still open. */
    @DeleteMapping("/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable String requestId) {
        serviceRequestFacade.cancel(AuthorizationUtil.getCurrentUser().getUserId(), requestId);
    }
}
