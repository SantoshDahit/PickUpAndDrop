package com.landgreet.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.landgreet.support.IntegrationTestBase;
import com.landgreet.support.TestAuthHelper;
import com.landgreet.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class BookingControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String bookingJson(String routeId, LocalDate date, int partySize, String matchPref) {
        return """
                {"routeId":"%s","travelDate":"%s","partySize":%d,"matchPref":"%s","intro":"hi"}
                """.formatted(routeId, date, partySize, matchPref);
    }

    @Test
    void groupBookingWithNoMatchesFoundsGroupOfOne() throws Exception {
        var user = dataHelper.createUser();
        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().plusDays(30), 1, "GROUP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").isNotEmpty())
                .andExpect(jsonPath("$.joinedExistingGroup").value(false));
    }

    @Test
    void secondBookingWithinSevenDaysJoinsSameGroup() throws Exception {
        var first = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(60), 1);

        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().plusDays(67), 1, "GROUP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(first.getGroupId()))
                .andExpect(jsonPath("$.joinedExistingGroup").value(true));
    }

    @Test
    void beyondWindowFoundsNewGroup() throws Exception {
        var first = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(100), 1);

        var result = mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().plusDays(108), 1, "GROUP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joinedExistingGroup").value(false))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(body).doesNotContain(first.getGroupId());
    }

    @Test
    void individualPreferenceNeverGetsGroup() throws Exception {
        dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(150), 1);

        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().plusDays(150), 1, "INDIVIDUAL")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").doesNotExist());
    }

    @Test
    void capacityCountsPartySizes() throws Exception {
        var big = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(200), 4);
        var fill = dataHelper.createGroupBooking(dataHelper.createUser(), LocalDate.now().plusDays(200), 2);
        org.assertj.core.api.Assertions.assertThat(fill.getGroupId()).isEqualTo(big.getGroupId());

        // Group is full (6 seats) — the next booking founds a new group.
        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().plusDays(200), 1, "GROUP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.joinedExistingGroup").value(false));
    }

    @Test
    void pastDateRejected() throws Exception {
        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(dataHelper.firstRoute().getId(), LocalDate.now().minusDays(1), 1, "GROUP")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BKG_BR_001"));
    }

    @Test
    void cancellingSomeoneElsesBookingIs404() throws Exception {
        var victim = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(victim, LocalDate.now().plusDays(250), 1);

        mockMvc.perform(delete("/v1/bookings/" + booking.getId())
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/v1/bookings/" + booking.getId())
                        .header("Authorization", authHelper.bearerFor(victim)))
                .andExpect(status().isNoContent());
    }
}
