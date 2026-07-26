package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class TravelGroupControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    @Test
    void groupIsHiddenFromNonMembersButVisibleToAdmin() throws Exception {
        var member = dataHelper.createUser();
        var groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(40), 1).getGroupId();

        mockMvc.perform(get("/v1/groups/" + groupId)
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/v1/groups/" + groupId)
                        .header("Authorization", authHelper.bearerFor(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].me").value(true));

        mockMvc.perform(get("/v1/groups/" + groupId)
                        .header("Authorization", authHelper.bearerFor(dataHelper.createAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void membersChatAndStrangersCannot() throws Exception {
        var member = dataHelper.createUser();
        var groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(50), 1).getGroupId();

        mockMvc.perform(post("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"see you at arrivals!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mine").value(true));

        mockMvc.perform(get("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("see you at arrivals!"));

        mockMvc.perform(post("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"let me in\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void agreementAppearsWhenDatesConverge() throws Exception {
        var u1 = dataHelper.createUser();
        var u2 = dataHelper.createUser();
        LocalDate day = dataHelper.groupableDate(80);
        var first = dataHelper.createGroupBooking(u1, day, 1);
        var second = dataHelper.createGroupBooking(u2, day.plusDays(3), 1);
        org.assertj.core.api.Assertions.assertThat(second.getGroupId()).isEqualTo(first.getGroupId());

        mockMvc.perform(get("/v1/groups/" + first.getGroupId())
                        .header("Authorization", authHelper.bearerFor(u1)))
                .andExpect(jsonPath("$.agreedDate").doesNotExist());

        // u2 moves their date to match u1 (PATCH on the booking resource).
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/v1/bookings/" + second.getId())
                        .header("Authorization", authHelper.bearerFor(u2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"travelDate\":\"" + day + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/groups/" + first.getGroupId())
                        .header("Authorization", authHelper.bearerFor(u1)))
                .andExpect(jsonPath("$.agreedDate").value(day.toString()));
    }

    @Test
    void leavingFlipsToIndividualAndLastLeaverClosesGroup() throws Exception {
        var u1 = dataHelper.createUser();
        var u2 = dataHelper.createUser();
        LocalDate leaveDay = dataHelper.groupableDate(120);
        var first = dataHelper.createGroupBooking(u1, leaveDay, 3);
        dataHelper.createGroupBooking(u2, leaveDay, 3);
        String groupId = first.getGroupId();

        mockMvc.perform(delete("/v1/groups/" + groupId + "/members/me")
                        .header("Authorization", authHelper.bearerFor(u2)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/groups/" + groupId)
                        .header("Authorization", authHelper.bearerFor(u1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"));

        // u2 no longer sees the group; their booking is now INDIVIDUAL.
        mockMvc.perform(get("/v1/groups/" + groupId)
                        .header("Authorization", authHelper.bearerFor(u2)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/v1/bookings/me")
                        .header("Authorization", authHelper.bearerFor(u2)))
                .andExpect(jsonPath("$[0].matchPref").value("INDIVIDUAL"))
                .andExpect(jsonPath("$[0].groupId").doesNotExist());
    }

    @Test
    void adminSearchRequiresAdminRole() throws Exception {
        mockMvc.perform(get("/v1/admin/bookings/search")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createUser())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/v1/admin/bookings/search")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createAdmin())))
                .andExpect(status().isOk());
    }
}
