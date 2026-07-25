package com.landgreet.controller;

import com.landgreet.dto.DriverDto;
import com.landgreet.service.DriverFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/admin/drivers")
@RequiredArgsConstructor
public class AdminDriverController {

    private final DriverFacade driverFacade;

    /** Register a driver. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DriverDto.Response create(@RequestBody @Valid DriverDto.PostRequest request) {
        return driverFacade.create(request);
    }

    /** Roster search (name contains / statusList / minSeats), paged. */
    @GetMapping("/search")
    public Page<DriverDto.SummaryResponse> search(
            @ModelAttribute DriverDto.SearchRequest searchRequest,
            Pageable pageable) {
        return driverFacade.search(searchRequest, pageable);
    }

    /** Driver detail. */
    @GetMapping("/{driverId}")
    public DriverDto.Response getById(@PathVariable String driverId) {
        return driverFacade.getById(driverId);
    }

    /** Partial update (null = keep). */
    @PatchMapping("/{driverId}")
    public DriverDto.Response update(
            @PathVariable String driverId,
            @RequestBody @Valid DriverDto.PatchRequest request) {
        return driverFacade.update(driverId, request);
    }

    /** ACTIVE ↔ INACTIVE. */
    @PatchMapping("/{driverId}/status")
    public DriverDto.Response updateStatus(
            @PathVariable String driverId,
            @RequestBody @Valid DriverDto.StatusPatchRequest request) {
        return driverFacade.updateStatus(driverId, request);
    }

    /** Soft delete; refused while upcoming rides exist. */
    @DeleteMapping("/{driverId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String driverId) {
        driverFacade.delete(driverId);
    }
}
