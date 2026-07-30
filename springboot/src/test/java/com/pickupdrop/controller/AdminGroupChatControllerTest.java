package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class AdminGroupChatControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String admin() {
        return authHelper.bearerFor(dataHelper.createAdmin());
    }

    @Test
    void chatIndexCarriesCountsAndTheLastMessage() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(40), 2).getGroupId();

        mockMvc.perform(post("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Anyone landing early?"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/admin/groups/chats").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')].messageCount").value(1))
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')].memberCount").value(1))
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')].seatsLeft").value(4))
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')].lastMessagePreview")
                        .value("Anyone landing early?"))
                .andExpect(jsonPath("$[?(@.id == '" + groupId + "')].lastMessageStaff").value(false));
    }

    @Test
    void adminSeesRealIdentitiesInDetail() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(41), 1).getGroupId();

        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/detail").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].email").value(member.getEmail()))
                .andExpect(jsonPath("$.members[0].name").value(member.getName()))
                .andExpect(jsonPath("$.members[0].bookingId").isNotEmpty());
    }

    @Test
    void adminCanReplyWithoutBeingAMemberAndTravellersSeeTheTeamName() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(42), 1).getGroupId();

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/messages")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Your driver is confirmed for Tuesday."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staff").value(true));

        // The traveller sees the team name, and it is never "mine" to them.
        mockMvc.perform(get("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staff").value(true))
                .andExpect(jsonPath("$[0].authorFirstName").value("Pickup & Drop team"))
                .andExpect(jsonPath("$[0].mine").value(false));
    }

    @Test
    void memberMessagesStayNonStaffAndOwned() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(43), 1).getGroupId();

        mockMvc.perform(post("/v1/groups/" + groupId + "/messages")
                        .header("Authorization", authHelper.bearerFor(member))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"See you at arrivals."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staff").value(false))
                .andExpect(jsonPath("$.mine").value(true));
    }

    @Test
    void blankOrOverlongReplyIsRejected() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(44), 1).getGroupId();

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/messages")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/messages")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminAddsAndRemovesAMember() throws Exception {
        LocalDate date = dataHelper.groupableDate(45);
        User first = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(first, date, 1).getGroupId();

        // A second traveller on the same route + week, riding alone for now.
        User second = dataHelper.createUser();
        String bookingId = dataHelper.createGroupBooking(second, date, 2).getId();
        mockMvc.perform(delete("/v1/admin/groups/" + groupId + "/members/" + bookingId)
                        .header("Authorization", admin()))
                .andExpect(status().isNoContent());   // detach from whatever group it picked

        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/candidates")
                        .header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.bookingId == '" + bookingId + "')]").exists());

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/members")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(bookingId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/detail").header("Authorization", admin()))
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.seatsLeft").value(3));

        mockMvc.perform(delete("/v1/admin/groups/" + groupId + "/members/" + bookingId)
                        .header("Authorization", admin()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/detail").header("Authorization", admin()))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andExpect(jsonPath("$.seatsLeft").value(5));
    }

    @Test
    void addingAnExistingMemberIsANoOp() throws Exception {
        User member = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(member, dataHelper.groupableDate(46), 1);

        mockMvc.perform(post("/v1/admin/groups/" + booking.getGroupId() + "/members")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(booking.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/groups/" + booking.getGroupId() + "/detail")
                        .header("Authorization", admin()))
                .andExpect(jsonPath("$.members.length()").value(1));
    }

    @Test
    void addingABookingFromAnotherWeekIsRejected() throws Exception {
        User first = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(first, dataHelper.groupableDate(47), 1).getGroupId();

        // +30 days lands in a different landing-week bucket.
        User other = dataHelper.createUser();
        String bookingId = dataHelper.createGroupBooking(other, dataHelper.groupableDate(77), 1).getId();

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/members")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(bookingId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_002"));
    }

    @Test
    void addingBeyondSeatCapacityIsRejected() throws Exception {
        LocalDate date = dataHelper.groupableDate(48);
        User first = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(first, date, 5).getGroupId();

        User second = dataHelper.createUser();
        String bookingId = dataHelper.createGroupBooking(second, date, 2).getId();
        mockMvc.perform(delete("/v1/admin/groups/" + groupId + "/members/" + bookingId)
                .header("Authorization", admin()));

        // 5 + 2 > MAX_SEATS(6): refused, and it must not be offered as a candidate.
        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/candidates")
                        .header("Authorization", admin()))
                .andExpect(jsonPath("$[?(@.bookingId == '" + bookingId + "')]").doesNotExist());

        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/members")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(bookingId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("GRP_BR_003"));
    }

    @Test
    void removingANonMemberIs404() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(49), 1).getGroupId();
        User other = dataHelper.createUser();
        String foreignBooking = dataHelper.createGroupBooking(other, dataHelper.groupableDate(79), 1).getId();

        mockMvc.perform(delete("/v1/admin/groups/" + groupId + "/members/" + foreignBooking)
                        .header("Authorization", admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void everyEndpointRefusesANonAdminToken() throws Exception {
        User member = dataHelper.createUser();
        var booking = dataHelper.createGroupBooking(member, dataHelper.groupableDate(51), 1);
        String groupId = booking.getGroupId();
        String userToken = authHelper.bearerFor(member);

        mockMvc.perform(get("/v1/admin/groups/chats").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/detail").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/messages").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/admin/groups/" + groupId + "/candidates").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/messages")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"let me in"}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/admin/groups/" + groupId + "/members")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookingId":"%s"}
                                """.formatted(booking.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/v1/admin/groups/" + groupId + "/members/" + booking.getId())
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminChatEndpointsDoNotLeakIntoTheCustomerGroupView() throws Exception {
        User member = dataHelper.createUser();
        String groupId = dataHelper.createGroupBooking(member, dataHelper.groupableDate(52), 1).getGroupId();

        // The member view keeps the 002 §4.5 privacy contract: first name only.
        mockMvc.perform(get("/v1/groups/" + groupId).header("Authorization", authHelper.bearerFor(member)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[0].firstName").isNotEmpty())
                .andExpect(jsonPath("$.members[0].email").doesNotExist())
                .andExpect(jsonPath("$.members[0].phone").doesNotExist())
                .andExpect(jsonPath("$.members[0].notes").doesNotExist());
    }
}
