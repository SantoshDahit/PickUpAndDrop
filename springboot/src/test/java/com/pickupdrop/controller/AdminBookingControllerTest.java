package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pickupdrop.entity.User;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({TestDataHelper.class, TestAuthHelper.class})
class AdminBookingControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void detailShowsCustomerAndCancelWorksOnce() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        User customer = dataHelper.createUser();
        String bookingId = dataHelper
                .createGroupBooking(customer, LocalDate.now().plusDays(10), 2).getId();

        mockMvc.perform(get("/v1/admin/bookings/" + bookingId).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customer.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.partySize").value(2))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(delete("/v1/admin/bookings/" + bookingId).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/bookings/" + bookingId).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(delete("/v1/admin/bookings/" + bookingId).header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BKG_BR_002"));
    }

    @Test
    void regularUsersAreRefused() throws Exception {
        String user = authHelper.bearerFor(dataHelper.createUser());

        mockMvc.perform(get("/v1/admin/bookings/some-id").header("Authorization", user))
                .andExpect(status().isForbidden());
    }
}
