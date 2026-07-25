package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class DriverAdminControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void adminCrudLifecycle() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());

        var created = mockMvc.perform(post("/v1/admin/drivers")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Kim Cheolsu","phone":"+82 10-9999-0001","licenseNo":"11-22-334455",
                                 "ownsVehicle":true,"vehicle":"Hyundai Staria","plateNo":"12가3456","seats":6}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/admin/drivers/search?name=Cheolsu").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].plateNo").value("12가3456"));

        mockMvc.perform(patch("/v1/admin/drivers/" + id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+82 10-9999-0002\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+82 10-9999-0002"))
                .andExpect(jsonPath("$.name").value("Kim Cheolsu")); // null = keep

        mockMvc.perform(patch("/v1/admin/drivers/" + id + "/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(delete("/v1/admin/drivers/" + id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/v1/admin/drivers/" + id).header("Authorization", admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void regularUserGets403() throws Exception {
        String user = authHelper.bearerFor(dataHelper.createUser());
        mockMvc.perform(get("/v1/admin/drivers/search").header("Authorization", user))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/admin/drivers").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"seats\":4}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRefusedWhileUpcomingRidesExist() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var driver = dataHelper.createDriver(6);
        var booking = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(300), 2);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/v1/admin/groups/" + booking.getGroupId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + driver.getId() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/v1/admin/drivers/" + driver.getId()).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DRV_BR_003"));

        mockMvc.perform(delete("/v1/admin/groups/" + booking.getGroupId() + "/driver")
                        .header("Authorization", admin))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/v1/admin/drivers/" + driver.getId()).header("Authorization", admin))
                .andExpect(status().isNoContent());
    }
}
