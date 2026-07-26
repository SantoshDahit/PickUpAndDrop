package com.pickupdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class DriverPortalControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void adminCreatesLoginAndDriverUsesPortal() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var driver = dataHelper.createDriver(6);

        mockMvc.perform(post("/v1/admin/drivers/" + driver.getId() + "/account")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kim-portal@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated());

        // login as the driver via the normal auth endpoint
        var login = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"kim-portal@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DRIVER"))
                .andReturn();
        String token = "Bearer " + new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(login.getResponse().getContentAsString()).get("accessToken").asText();

        mockMvc.perform(get("/v1/drivers/me").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(driver.getId()));

        // PATCH updates own fields; capacity/status not in the DTO at all
        mockMvc.perform(patch("/v1/drivers/me").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"+82 10-5555-5555\",\"plateNo\":\"99바9999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+82 10-5555-5555"))
                .andExpect(jsonPath("$.plateNo").value("99바9999"))
                .andExpect(jsonPath("$.seats").value(6));
    }

    @Test
    void ridesListRollsUpGroupWithPassengerContacts() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var driver = dataHelper.createDriver(6);
        var account = dataHelper.createDriverAccount(driver);
        String token = authHelper.bearerFor(account);

        // empty before assignment
        mockMvc.perform(get("/v1/drivers/me/rides").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // a group with two members (dates within window) assigned to this driver
        var u1 = dataHelper.createUser();
        LocalDate day = dataHelper.groupableDate(21);
        var b1 = dataHelper.createGroupBooking(u1, day, 1);
        dataHelper.createGroupBooking(dataHelper.createUser(), day.plusDays(2), 2);
        mockMvc.perform(put("/v1/admin/groups/" + b1.getGroupId() + "/driver")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"" + driver.getId() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/drivers/me/rides").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rideType").value("GROUP"))
                .andExpect(jsonPath("$[0].totalSeats").value(3))
                .andExpect(jsonPath("$[0].passengers.length()").value(2))
                .andExpect(jsonPath("$[0].passengers[0].firstName").isNotEmpty())
                // email/intro never leak to drivers
                .andExpect(jsonPath("$[0].passengers[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].passengers[0].intro").doesNotExist());
    }

    @Test
    void roleGatesBothDirections() throws Exception {
        var driver = dataHelper.createDriver(4);
        String driverToken = authHelper.bearerFor(dataHelper.createDriverAccount(driver));
        String userToken = authHelper.bearerFor(dataHelper.createUser());

        mockMvc.perform(get("/v1/drivers/me").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/admin/drivers/search").header("Authorization", driverToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateAccountAndTakenEmailRefused() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        var driver = dataHelper.createDriver(4);
        dataHelper.createDriverAccount(driver);

        mockMvc.perform(post("/v1/admin/drivers/" + driver.getId() + "/account")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"second@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DRV_BR_004"));

        var other = dataHelper.createDriver(4);
        var existingUser = dataHelper.createUser();
        mockMvc.perform(post("/v1/admin/drivers/" + other.getId() + "/account")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + existingUser.getEmail() + "\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("USR_BR_001"));
    }
}
