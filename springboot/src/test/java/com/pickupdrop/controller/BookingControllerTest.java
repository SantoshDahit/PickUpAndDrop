package com.pickupdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestAuthHelper;
import com.pickupdrop.support.TestDataHelper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class BookingControllerTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String bookingJson(LocalDate date, int partySize) {
        return """
                {"routeId":"%s","travelDate":"%s","partySize":%d,"matchPref":"GROUP","intro":"hi"}
                """.formatted(dataHelper.firstRoute().getId(), date, partySize);
    }

    private String createBooking(String bearer, LocalDate date, int partySize) throws Exception {
        var result = mockMvc.perform(post("/v1/bookings").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(date, partySize)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").doesNotExist()) // book first, group second
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void bookThenStartGroupThenSecondBookerJoinsViaSuggestion() throws Exception {
        LocalDate day = dataHelper.groupableDate(40);
        String a = authHelper.bearerFor(dataHelper.createUser());
        String bookingA = createBooking(a, day, 1);

        // no groups yet in this landing week
        mockMvc.perform(get("/v1/bookings/" + bookingA + "/group-suggestions").header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(0))
                .andExpect(jsonPath("$.weekStart").isNotEmpty());

        // start a new group
        var started = mockMvc.perform(put("/v1/bookings/" + bookingA + "/group").header("Authorization", a)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").isNotEmpty())
                .andReturn();
        String groupId = JSON.readTree(started.getResponse().getContentAsString()).get("groupId").asText();

        // second booker, same week (+3 days) sees it suggested and joins
        String b = authHelper.bearerFor(dataHelper.createUser());
        String bookingB = createBooking(b, day.plusDays(3), 2);
        mockMvc.perform(get("/v1/bookings/" + bookingB + "/group-suggestions").header("Authorization", b))
                .andExpect(jsonPath("$.groups[0].id").value(groupId))
                .andExpect(jsonPath("$.groups[0].memberCount").value(1))
                .andExpect(jsonPath("$.groups[0].seatsLeft").value(5))
                // never any personal data in suggestions
                .andExpect(jsonPath("$.groups[0].firstName").doesNotExist());
        mockMvc.perform(put("/v1/bookings/" + bookingB + "/group").header("Authorization", b)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + groupId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(groupId));
    }

    @Test
    void otherWeeksGroupsAreNeitherSuggestedNorJoinable() throws Exception {
        LocalDate week1 = dataHelper.groupableDate(70);
        var founded = dataHelper.createGroupBooking(dataHelper.createUser(), week1, 1);

        String b = authHelper.bearerFor(dataHelper.createUser());
        String booking = createBooking(b, week1.plusDays(10), 1); // next landing week
        String suggestions = mockMvc.perform(
                        get("/v1/bookings/" + booking + "/group-suggestions").header("Authorization", b))
                .andReturn().getResponse().getContentAsString();
        assertThat(suggestions).doesNotContain(founded.getGroupId());

        mockMvc.perform(put("/v1/bookings/" + booking + "/group").header("Authorization", b)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + founded.getGroupId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_002"));
    }

    @Test
    void capacityDropsFullGroupsFromSuggestions() throws Exception {
        LocalDate day = dataHelper.groupableDate(100);
        var big = dataHelper.createGroupBooking(dataHelper.createUser(), day, 4);
        dataHelper.createGroupBooking(dataHelper.createUser(), day, 2); // 6/6 seats

        String c = authHelper.bearerFor(dataHelper.createUser());
        String booking = createBooking(c, day, 1);
        String suggestions = mockMvc.perform(
                        get("/v1/bookings/" + booking + "/group-suggestions").header("Authorization", c))
                .andReturn().getResponse().getContentAsString();
        assertThat(suggestions).doesNotContain(big.getGroupId());

        mockMvc.perform(put("/v1/bookings/" + booking + "/group").header("Authorization", c)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + big.getGroupId() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_003"));
    }

    @Test
    void switchingBetweenSameWeekGroupsFreesSeats() throws Exception {
        LocalDate day = dataHelper.groupableDate(130);
        var g1 = dataHelper.createGroupBooking(dataHelper.createUser(), day, 1);

        String b = authHelper.bearerFor(dataHelper.createUser());
        String booking = createBooking(b, day, 1);
        // start own group, then switch into g1
        mockMvc.perform(put("/v1/bookings/" + booking + "/group").header("Authorization", b)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"groupId\":null}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/v1/bookings/" + booking + "/group").header("Authorization", b)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupId\":\"" + g1.getGroupId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(g1.getGroupId()));
    }

    @Test
    void dateChangeAcrossWeeksDetachesFromGroup() throws Exception {
        LocalDate day = dataHelper.groupableDate(160);
        var user = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(user, day, 1);
        String bearer = authHelper.bearerFor(user);

        // inside the week: membership survives
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/v1/bookings/" + booking.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelDate\":\"" + day.plusDays(2) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").value(booking.getGroupId()));

        // across weeks: detached, chat boundary enforced
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/v1/bookings/" + booking.getId()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelDate\":\"" + day.plusDays(10) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupId").doesNotExist());
        mockMvc.perform(get("/v1/groups/" + booking.getGroupId()).header("Authorization", bearer))
                .andExpect(status().isNotFound());
    }

    @Test
    void suggestionsAreOwnerOnly() throws Exception {
        var owner = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(owner, dataHelper.groupableDate(190), 1);
        mockMvc.perform(get("/v1/bookings/" + booking.getId() + "/group-suggestions")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void fifthWeekIsItsOwnBucket() throws Exception {
        // find a future month with 31 days; day 30 (W5) vs day 1 next month (W1)
        LocalDate probe = LocalDate.now().plusMonths(2).withDayOfMonth(1);
        while (probe.lengthOfMonth() < 31) probe = probe.plusMonths(1);
        LocalDate w5 = probe.withDayOfMonth(30);
        LocalDate nextW1 = probe.plusMonths(1).withDayOfMonth(1);

        var w5Group = dataHelper.createGroupBooking(dataHelper.createUser(), w5, 1);
        String b = authHelper.bearerFor(dataHelper.createUser());
        String booking = createBooking(b, nextW1, 1);
        String suggestions = mockMvc.perform(
                        get("/v1/bookings/" + booking + "/group-suggestions").header("Authorization", b))
                .andReturn().getResponse().getContentAsString();
        assertThat(suggestions).doesNotContain(w5Group.getGroupId());
    }

    @Test
    void pastDateRejected() throws Exception {
        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingJson(LocalDate.now().minusDays(1), 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BKG_BR_001"));
    }

    @Test
    void cancellingSomeoneElsesBookingIs404() throws Exception {
        var victim = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(victim, dataHelper.groupableDate(220), 1);

        mockMvc.perform(delete("/v1/bookings/" + booking.getId())
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/v1/bookings/" + booking.getId())
                        .header("Authorization", authHelper.bearerFor(victim)))
                .andExpect(status().isNoContent());
    }
}
