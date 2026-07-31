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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

@Import({TestDataHelper.class, TestAuthHelper.class})
class AdminSupportControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String admin() {
        return authHelper.bearerFor(dataHelper.createAdmin());
    }

    private void travellerWrites(User user, String body) throws Exception {
        mockMvc.perform(post("/v1/support/messages")
                        .header("Authorization", authHelper.bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void inboxShowsWhoIsWaitingAndClearsOnceRead() throws Exception {
        User traveller = dataHelper.createUser();
        travellerWrites(traveller, "first");
        travellerWrites(traveller, "second");
        String adminToken = admin();

        mockMvc.perform(get("/v1/admin/support").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')].messageCount").value(2))
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')].unread").value(2))
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')].customerEmail")
                        .value(traveller.getEmail()));

        // Opening the thread marks their messages read.
        mockMvc.perform(get("/v1/admin/support/" + traveller.getId() + "/messages")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));

        mockMvc.perform(get("/v1/admin/support").header("Authorization", adminToken))
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')].unread").value(0));
    }

    @Test
    void replyLandsOnTheTravellersThreadNotTheAdmins() throws Exception {
        User traveller = dataHelper.createUser();
        travellerWrites(traveller, "hello");
        String adminToken = admin();

        mockMvc.perform(post("/v1/admin/support/" + traveller.getId() + "/messages")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hello back"}
                                """))
                .andExpect(status().isCreated());

        // The thread is keyed by the traveller, so the admin has none of their own.
        mockMvc.perform(get("/v1/admin/support").header("Authorization", adminToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(traveller.getId()));
    }

    @Test
    void aTravellerCannotReachTheInboxOrAnotherThread() throws Exception {
        User traveller = dataHelper.createUser();
        travellerWrites(traveller, "hi");
        String userToken = authHelper.bearerFor(dataHelper.createUser());

        mockMvc.perform(get("/v1/admin/support").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/v1/admin/support/" + traveller.getId() + "/messages")
                        .header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/admin/support/" + traveller.getId() + "/messages")
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"pretending to be staff"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void threadForAnUnknownUserIs404() throws Exception {
        mockMvc.perform(get("/v1/admin/support/nobody/messages").header("Authorization", admin()))
                .andExpect(status().isNotFound());
    }

    /** Same reasoning as the service-request queue: nobody left to reply to. */
    @Test
    void deletedAccountsDropOutOfTheInbox() throws Exception {
        User traveller = dataHelper.createUser();
        travellerWrites(traveller, "before I go");
        String adminToken = admin();

        mockMvc.perform(get("/v1/admin/support").header("Authorization", adminToken))
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')]").exists());

        mockMvc.perform(delete("/v1/users/me")
                        .header("Authorization", authHelper.bearerFor(traveller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password":"%s"}
                                """.formatted(TestDataHelper.PASSWORD)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/admin/support").header("Authorization", adminToken))
                .andExpect(jsonPath("$[?(@.userId == '" + traveller.getId() + "')]").doesNotExist());
    }
}
