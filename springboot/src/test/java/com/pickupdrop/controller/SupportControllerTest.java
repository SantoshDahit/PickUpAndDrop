package com.pickupdrop.controller;

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
class SupportControllerTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private void send(String token, String body) throws Exception {
        mockMvc.perform(post("/v1/support/messages")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + body + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void aTravellerWithNoGroupCanStillReachTheTeam() throws Exception {
        // The whole point: no booking, no group, no group chat — but a thread.
        User traveller = dataHelper.createUser();
        String token = authHelper.bearerFor(traveller);

        mockMvc.perform(get("/v1/support/messages").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(0));

        send(token, "Hi, my flight lands at 6am — is that ok?");

        mockMvc.perform(get("/v1/support/messages").header("Authorization", token))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].mine").value(true))
                .andExpect(jsonPath("$.messages[0].staff").value(false))
                .andExpect(jsonPath("$.messages[0].authorName").value(traveller.getName()));
    }

    @Test
    void threadsAreIsolatedBetweenTravellers() throws Exception {
        User mine = dataHelper.createUser();
        User theirs = dataHelper.createUser();
        send(authHelper.bearerFor(mine), "my question");
        send(authHelper.bearerFor(theirs), "their question");

        mockMvc.perform(get("/v1/support/messages").header("Authorization", authHelper.bearerFor(mine)))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].body").value("my question"));
    }

    @Test
    void staffReplyShowsAsTheTeamAndNotAsMine() throws Exception {
        User traveller = dataHelper.createUser();
        String token = authHelper.bearerFor(traveller);
        send(token, "Any update?");

        mockMvc.perform(post("/v1/admin/support/" + traveller.getId() + "/messages")
                        .header("Authorization", authHelper.bearerFor(dataHelper.createAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Yes — your driver is booked."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staff").value(true));

        mockMvc.perform(get("/v1/support/messages").header("Authorization", token))
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[1].staff").value(true))
                .andExpect(jsonPath("$.messages[1].mine").value(false))
                .andExpect(jsonPath("$.messages[1].authorName").value("Pickup & Drop team"));
    }

    @Test
    void blankAndOverlongMessagesAreRejected() throws Exception {
        String token = authHelper.bearerFor(dataHelper.createUser());

        mockMvc.perform(post("/v1/support/messages")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/support/messages")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void supportNeedsALogin() throws Exception {
        mockMvc.perform(get("/v1/support/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/v1/support/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"hello"}
                                """))
                .andExpect(status().isUnauthorized());
    }
}
