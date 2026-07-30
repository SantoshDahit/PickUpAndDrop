package com.pickupdrop.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class AdminServiceRequestControllerTest extends IntegrationTestBase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private TestAuthHelper authHelper;

    private String admin() {
        return authHelper.bearerFor(dataHelper.createAdmin());
    }

    private String createFor(User user) throws Exception {
        String json = mockMvc.perform(post("/v1/service-requests")
                        .header("Authorization", authHelper.bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"SIM_CARD","arrivalDate":"%s","airport":"ICN",
                                 "detail":"30 days / 10GB","contact":"+977 98..."}
                                """.formatted(LocalDate.now().plusDays(15))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return MAPPER.readTree(json).get("id").asText();
    }

    @Test
    void queueCarriesTheTravellerIdentity() throws Exception {
        User traveller = dataHelper.createUser();
        String requestId = createFor(traveller);

        mockMvc.perform(get("/v1/admin/service-requests").header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')].customerEmail")
                        .value(traveller.getEmail()))
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')].customerName")
                        .value(traveller.getName()))
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')].status").value("REQUESTED"));
    }

    @Test
    void queueFiltersByStatus() throws Exception {
        String requestId = createFor(dataHelper.createUser());
        String adminToken = admin();

        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/admin/service-requests?status=CONFIRMED")
                        .header("Authorization", adminToken))
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')]").exists());
        mockMvc.perform(get("/v1/admin/service-requests?status=REQUESTED")
                        .header("Authorization", adminToken))
                .andExpect(jsonPath("$[?(@.id == '" + requestId + "')]").doesNotExist());
    }

    @Test
    void statusWalksTheWorkflowAndRefusesIllegalMoves() throws Exception {
        String requestId = createFor(dataHelper.createUser());
        String adminToken = admin();

        for (String next : new String[]{"CONFIRMED", "DELIVERED"}) {
            mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + next + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(next));
        }

        // DELIVERED is terminal — no going back, and no cancelling afterwards.
        for (String illegal : new String[]{"CONFIRMED", "REQUESTED", "CANCELLED"}) {
            mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                            .header("Authorization", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"" + illegal + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("SVC_BR_001"));
        }
    }

    @Test
    void operatorCanCancelWithANote() throws Exception {
        String requestId = createFor(dataHelper.createUser());

        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED","adminNote":"Traveller changed plans"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.adminNote").value("Traveller changed plans"));
    }

    @Test
    void aNoteAloneLeavesTheStatusAlone() throws Exception {
        String requestId = createFor(dataHelper.createUser());

        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"adminNote":"Waiting on carrier stock"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.adminNote").value("Waiting on carrier stock"));
    }

    @Test
    void unknownRequestIs404() throws Exception {
        mockMvc.perform(patch("/v1/admin/service-requests/does-not-exist")
                        .header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CONFIRMED"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("SVC_NF_001"));
    }

    @Test
    void everyAdminEndpointRefusesATravellerToken() throws Exception {
        User traveller = dataHelper.createUser();
        String requestId = createFor(traveller);
        String userToken = authHelper.bearerFor(traveller);

        mockMvc.perform(get("/v1/admin/service-requests").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/v1/admin/service-requests/" + requestId)
                        .header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"DELIVERED"}
                                """))
                .andExpect(status().isForbidden());
    }
}
