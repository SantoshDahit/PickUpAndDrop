package com.pickupdrop.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class OpenRideControllerTest extends IntegrationTestBase {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String publishRide(String admin, LocalDate targetDate) throws Exception {
        var result = mockMvc.perform(post("/v1/admin/groups")
                        .header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"" + dataHelper.firstRoute().getId() + "\"," +
                                "\"targetDate\":\"" + targetDate + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatsLeft").value(6))
                .andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String joinJson(LocalDate date, int party, String groupId) {
        return "{\"routeId\":\"" + dataHelper.firstRoute().getId() + "\",\"travelDate\":\"" + date +
                "\",\"partySize\":" + party + ",\"matchPref\":\"INDIVIDUAL\",\"groupId\":\"" + groupId + "\"}";
    }

    @Test
    void publishBrowseJoinWindowFlow() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        LocalDate target = LocalDate.now().plusDays(130);
        String rideId = publishRide(admin, target);

        // any signed-in user can browse; the card carries no personal data
        String user = authHelper.bearerFor(dataHelper.createUser());
        var browse = mockMvc.perform(get("/v1/groups/open").header("Authorization", user))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(browse).contains(rideId);
        assertThat(browse).doesNotContain("firstName").doesNotContain("intro");

        // join within window (matchPref forced to GROUP by the join)
        mockMvc.perform(post("/v1/bookings").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(target.plusDays(2), 2, rideId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.groupId").value(rideId))
                .andExpect(jsonPath("$.matchPref").value("GROUP"))
                .andExpect(jsonPath("$.joinedExistingGroup").value(true));

        // seats reflect on the browse card
        mockMvc.perform(get("/v1/groups/open").header("Authorization", user))
                .andExpect(jsonPath("$.[?(@.id == '" + rideId + "')].seatsLeft").value(4));

        // out-of-window join refused (target anchors the span)
        mockMvc.perform(post("/v1/bookings")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(target.plusDays(10), 1, rideId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_002"));
    }

    @Test
    void organicGroupsAreNeverJoinableOrBrowsable() throws Exception {
        var member = dataHelper.createUser();
        var organic = dataHelper.createGroupBooking(member, LocalDate.now().plusDays(175), 1);

        String stranger = authHelper.bearerFor(dataHelper.createUser());
        mockMvc.perform(post("/v1/bookings").header("Authorization", stranger)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(LocalDate.now().plusDays(175), 1, organic.getGroupId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_001"));

        String browse = mockMvc.perform(get("/v1/groups/open").header("Authorization", stranger))
                .andReturn().getResponse().getContentAsString();
        assertThat(browse).doesNotContain(organic.getGroupId());
    }

    @Test
    void organicMatchingSeedsIntoPublishedRide() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        LocalDate target = LocalDate.now().plusDays(165);
        String rideId = publishRide(admin, target);

        // plain GROUP booking near the target auto-joins the empty published ride
        var outcome = dataHelper.createGroupBooking(dataHelper.createUser(), target.minusDays(2), 1);
        assertThat(outcome.getGroupId()).isEqualTo(rideId);
        assertThat(outcome.getJoinedExistingGroup()).isTrue();
    }

    @Test
    void seatCapacityGuard() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        LocalDate target = LocalDate.now().plusDays(230);
        String rideId = publishRide(admin, target);

        mockMvc.perform(post("/v1/bookings").header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(target, 4, rideId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/bookings").header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(target, 3, rideId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_003"));
    }

    @Test
    void closeGuardsAndEmptyRideStaysOpen() throws Exception {
        String admin = authHelper.bearerFor(dataHelper.createAdmin());
        LocalDate target = LocalDate.now().plusDays(290);
        String rideId = publishRide(admin, target);

        String user = authHelper.bearerFor(dataHelper.createUser());
        var join = mockMvc.perform(post("/v1/bookings").header("Authorization", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(joinJson(target, 1, rideId)))
                .andExpect(status().isCreated())
                .andReturn();
        String bookingId = JSON.readTree(join.getResponse().getContentAsString()).get("id").asText();

        // close refused with members
        mockMvc.perform(patch("/v1/admin/groups/" + rideId + "/close").header("Authorization", admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_004"));

        // member cancels — a published ride stays OPEN (browsable), unlike organic groups
        mockMvc.perform(delete("/v1/bookings/" + bookingId).header("Authorization", user))
                .andExpect(status().isNoContent());
        String browse = mockMvc.perform(get("/v1/groups/open").header("Authorization", user))
                .andReturn().getResponse().getContentAsString();
        assertThat(browse).contains(rideId);

        // now close works and it disappears
        mockMvc.perform(patch("/v1/admin/groups/" + rideId + "/close").header("Authorization", admin))
                .andExpect(status().isNoContent());
        browse = mockMvc.perform(get("/v1/groups/open").header("Authorization", user))
                .andReturn().getResponse().getContentAsString();
        assertThat(browse).doesNotContain(rideId);
    }

    @Test
    void publishRequiresAdmin() throws Exception {
        mockMvc.perform(post("/v1/admin/groups")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"routeId\":\"x\",\"targetDate\":\"2027-01-01\"}"))
                .andExpect(status().isForbidden());
    }
}
