package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.enums.DriverStatus;
import com.pickupdrop.service.DriverFacade;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class DriverAssignmentControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Autowired
    private DriverFacade driverFacade;

    private String assignJson(String driverId) {
        return "{\"driverId\":\"" + driverId + "\"}";
    }

    @Test
    void groupAssignmentVisibleToMembersWithoutLicense() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var member = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(member, LocalDate.now().plusDays(320), 2);
        var driver = dataHelper.createDriver(6);

        mockMvc.perform(put("/v1/admin/groups/" + booking.getGroupId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson(driver.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/groups/" + booking.getGroupId())
                        .header("Authorization", authHelper.bearerFor(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driver.name").value(driver.getName()))
                .andExpect(jsonPath("$.driver.plateNo").value(driver.getPlateNo()))
                .andExpect(jsonPath("$.driver.licenseNo").doesNotExist());

        // effective driver also appears on the member's booking list
        mockMvc.perform(get("/v1/bookings/me").header("Authorization", authHelper.bearerFor(member)))
                .andExpect(jsonPath("$[0].driver.name").value(driver.getName()));
    }

    @Test
    void seatCapacityIsValidated() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var booking = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(340), 5);
        var smallCar = dataHelper.createDriver(4);

        mockMvc.perform(put("/v1/admin/groups/" + booking.getGroupId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson(smallCar.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DRV_BR_002"));
    }

    @Test
    void inactiveDriverIsRefused() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var booking = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(355), 1);
        var driver = dataHelper.createDriver(6);
        driverFacade.updateStatus(driver.getId(), new DriverDto.StatusPatchRequest(DriverStatus.INACTIVE));

        mockMvc.perform(put("/v1/admin/groups/" + booking.getGroupId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson(driver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DRV_BR_001"));
    }

    @Test
    void individualBookingAssignmentAndGroupedRefusal() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var solo = dataHelper.createUser();
        var driver = dataHelper.createDriver(4);

        // individual booking via facade helper (INDIVIDUAL pref)
        var response = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(solo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"" + dataHelper.firstRoute().getId() + "\"," +
                                "\"travelDate\":\"" + LocalDate.now().plusDays(270) + "\"," +
                                "\"partySize\":2,\"matchPref\":\"INDIVIDUAL\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/v1/admin/bookings/" + bookingId + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson(driver.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/bookings/me").header("Authorization", authHelper.bearerFor(solo)))
                .andExpect(jsonPath("$[0].driver.plateNo").value(driver.getPlateNo()));

        // grouped booking refuses direct assignment
        var grouped = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(280), 1);
        mockMvc.perform(put("/v1/admin/bookings/" + grouped.getId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assignJson(driver.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BKG_BR_003"));
    }
}
