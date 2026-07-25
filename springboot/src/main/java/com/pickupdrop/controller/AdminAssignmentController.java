package com.pickupdrop.controller;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.service.DriverFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Ride dispatch: driver ↔ group / individual booking. ADMIN via route rule. */
@RestController
@RequiredArgsConstructor
public class AdminAssignmentController {

    private final DriverFacade driverFacade;

    /** Assign (or replace) the driver on a travel group. */
    @PutMapping("/v1/admin/groups/{groupId}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignToGroup(
            @PathVariable String groupId,
            @RequestBody @Valid DriverDto.AssignRequest request) {
        driverFacade.assignToGroup(groupId, request);
    }

    /** Unassign the group's driver. */
    @DeleteMapping("/v1/admin/groups/{groupId}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignFromGroup(@PathVariable String groupId) {
        driverFacade.unassignFromGroup(groupId);
    }

    /** Assign (or replace) the driver on an individual booking. */
    @PutMapping("/v1/admin/bookings/{bookingId}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignToBooking(
            @PathVariable String bookingId,
            @RequestBody @Valid DriverDto.AssignRequest request) {
        driverFacade.assignToBooking(bookingId, request);
    }

    /** Unassign the booking's driver. */
    @DeleteMapping("/v1/admin/bookings/{bookingId}/driver")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignFromBooking(@PathVariable String bookingId) {
        driverFacade.unassignFromBooking(bookingId);
    }
}
