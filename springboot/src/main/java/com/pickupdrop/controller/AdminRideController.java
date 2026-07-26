package com.pickupdrop.controller;

import com.pickupdrop.dto.TravelGroupDto;
import com.pickupdrop.service.TravelGroupFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Admin-published rides travellers can browse and join. ADMIN via route rule. */
@RestController
@RequestMapping("/v1/admin/groups")
@RequiredArgsConstructor
public class AdminRideController {

    private final TravelGroupFacade travelGroupFacade;

    /** Publish a ride: route + advertised landing day. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TravelGroupDto.OpenRideResponse publish(
            @RequestBody @Valid TravelGroupDto.AdminPostRequest request) {
        return travelGroupFacade.publishRide(request);
    }

    /** Unpublish; refused while active members remain. */
    @PatchMapping("/{groupId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable String groupId) {
        travelGroupFacade.closeRide(groupId);
    }
}
