package com.pickupdrop.controller;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.service.DriverFacade;
import com.pickupdrop.util.AuthorizationUtil;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Driver self-service. DRIVER role via route rule; identity from the token. */
@RestController
@RequestMapping("/v1/drivers")
@RequiredArgsConstructor
public class DriverPortalController {

    private final DriverFacade driverFacade;

    /** My roster profile. */
    @GetMapping("/me")
    public DriverDto.Response getMe() {
        return driverFacade.getMyProfile(AuthorizationUtil.getCurrentUser().getUserId());
    }

    /** Update the facts only I know day-to-day: phone, vehicle, plate. */
    @PatchMapping("/me")
    public DriverDto.Response updateMe(@RequestBody @Valid DriverDto.MePatchRequest request) {
        return driverFacade.updateMyProfile(AuthorizationUtil.getCurrentUser().getUserId(), request);
    }

    /** My upcoming assigned rides with passenger details. */
    @GetMapping("/me/rides")
    public List<DriverDto.RideResponse> getMyRides() {
        return driverFacade.getMyRides(AuthorizationUtil.getCurrentUser().getUserId());
    }
}
